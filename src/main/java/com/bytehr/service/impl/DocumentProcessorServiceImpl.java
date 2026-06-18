package com.bytehr.service.impl;

import com.bytehr.config.SecurityProperties;
import com.bytehr.model.Document;
import com.bytehr.model.DocumentChunk;
import com.bytehr.repository.DocumentChunkRepository;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessorServiceImpl implements DocumentProcessorService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final LanguageDetectionService languageDetectionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final RedactionService redactionService;
    private final SecurityProperties securityProperties;
    private final JdbcTemplate jdbcTemplate;

    @Value("${document.chunk-size}")
    private int chunkSize;

    @Value("${document.chunk-overlap}")
    private int chunkOverlap;

    @Override
    @Transactional
    public void processDocument(Document document, byte[] fileContent) {
        log.info("[Processor] Processing document: '{}' ({} bytes)", document.getName(), fileContent.length);

        // Step 1: Extract text and page count using Tika
        ExtractionResult extraction = extractWithMetadata(fileContent, document.getName());
        if (extraction.text().isBlank()) {
            log.warn("[Processor] No text extracted from '{}' — skipping", document.getName());
            return;
        }
        log.info("[Processor] Extracted: '{}' — chars={}, pages={}",
                document.getName(), extraction.text().length(), extraction.pageCount());

        // Step 2: Detect language
        String language = languageDetectionService.detectLanguage(extraction.text());
        document.setLanguage(language);
        log.debug("[Processor] Detected language='{}' for '{}'", language, document.getName());

        // Step 3: Redact PII before any data enters the vector DB
        String text = extraction.text();
        if (securityProperties.getRedaction().isEnabled()) {
            text = redactionService.redact(text);
            log.info("[Processor] PII redaction applied to '{}'", document.getName());
        }

        // Step 4: Remove old chunks
        chunkRepository.deleteByDocumentId(document.getId());

        // Step 5: Chunk
        List<String> chunkTexts = chunkingService.chunk(text, chunkSize, chunkOverlap);
        int totalChunks = chunkTexts.size();
        log.info("[Processor] '{}' → {} chunks (size={}, overlap={})",
                document.getName(), totalChunks, chunkSize, chunkOverlap);

        int embeddingSuccesses = 0;
        int embeddingFailures = 0;

        // Step 6: Embed and store each chunk
        for (int i = 0; i < totalChunks; i++) {
            String chunkContent = chunkTexts.get(i);

            // Estimate page number from chunk position
            int estimatedPage = totalChunks > 0
                    ? Math.max(1, (i * extraction.pageCount() / totalChunks) + 1)
                    : 1;

            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunkContent)
                    .chunkIndex(i)
                    .pageNumber(estimatedPage)
                    .build();
            chunk = chunkRepository.saveAndFlush(chunk);

            log.debug("[Embedding] Requesting chunk {}/{} of '{}': chunkId={}, textLen={}, page={}",
                    i + 1, totalChunks, document.getName(), chunk.getId(), chunkContent.length(), estimatedPage);

            try {
                float[] embedding = embeddingService.generateEmbedding(chunkContent);
                log.debug("[Embedding] Received: dim={}", embedding.length);

                String vectorLiteral = toVectorLiteral(embedding);
                int rows = jdbcTemplate.update(
                        "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
                        vectorLiteral, chunk.getId());

                if (rows == 1) {
                    embeddingSuccesses++;
                } else {
                    log.error("[Embedding] FAILED to persist embedding for chunk id={}: rowsUpdated={}",
                            chunk.getId(), rows);
                    embeddingFailures++;
                }
            } catch (Exception e) {
                log.error("[Embedding] Exception on chunk {}/{} of '{}': {}",
                        i + 1, totalChunks, document.getName(), e.getMessage(), e);
                embeddingFailures++;
            }
        }

        log.info("[Processor] '{}' complete: {}/{} embeddings OK, {} failed",
                document.getName(), embeddingSuccesses, totalChunks, embeddingFailures);

        // Step 7: Update sync timestamp
        document.setLastSync(Instant.now());
        documentRepository.save(document);
    }

    /**
     * Extracts text and page count from a document using Tika's AutoDetectParser.
     * Supports PDF, DOCX, XLSX, PPTX, TXT, MD and all other Tika-supported formats.
     * Falls back gracefully on extraction errors.
     */
    private ExtractionResult extractWithMetadata(byte[] content, String filename) {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1); // no char limit
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            try (ByteArrayInputStream stream = new ByteArrayInputStream(content)) {
                parser.parse(stream, handler, metadata, new ParseContext());
            }
            String text = handler.toString();
            int pageCount = resolvePageCount(metadata, filename);
            return new ExtractionResult(text, pageCount);
        } catch (Exception e) {
            log.error("[Processor] Text extraction failed for '{}': {}", filename, e.getMessage());
            return new ExtractionResult("", 1);
        }
    }

    private int resolvePageCount(Metadata metadata, String filename) {
        // Try PDF-specific page count first
        for (String key : new String[]{
                "xmpTPg:NPages", "pdf:PDFVersion", "meta:page-count",
                "Page-Count", "cp:revision"}) {
            String val = metadata.get(key);
            if (val != null) {
                try {
                    int n = Integer.parseInt(val.trim());
                    if (n > 0) return n;
                } catch (NumberFormatException ignored) {}
            }
        }
        // Check for PDF pages via the standard Tika Office key
        String pages = metadata.get(org.apache.tika.metadata.Office.PAGE_COUNT);
        if (pages != null) {
            try { return Math.max(1, Integer.parseInt(pages.trim())); } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /** Carries extracted text and page count from a single Tika parse pass. */
    private record ExtractionResult(String text, int pageCount) {}
}
