package com.bytehr.service.impl;

import com.bytehr.model.Document;
import com.bytehr.model.DocumentChunk;
import com.bytehr.repository.DocumentChunkRepository;
import com.bytehr.repository.DocumentRepository;
import com.bytehr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
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
    private final JdbcTemplate jdbcTemplate;
    private final Tika tika;

    @Value("${document.chunk-size}")
    private int chunkSize;

    @Value("${document.chunk-overlap}")
    private int chunkOverlap;

    @Override
    @Transactional
    public void processDocument(Document document, byte[] fileContent) {
        log.info("Processing document: {}", document.getName());

        // Step 1: Extract text
        String text = extractText(fileContent, document.getName());
        if (text.isBlank()) {
            log.warn("No text extracted from document: {}", document.getName());
            return;
        }

        // Step 2: Detect language
        String language = languageDetectionService.detectLanguage(text);
        document.setLanguage(language);
        log.debug("[Processor] Detected language='{}' for document '{}'", language, document.getName());

        // Step 3: Remove old chunks for this document
        chunkRepository.deleteByDocumentId(document.getId());

        // Step 4: Chunk content
        List<String> chunkTexts = chunkingService.chunk(text, chunkSize, chunkOverlap);
        log.info("Document '{}' split into {} chunks (chunkSize={}, overlap={})",
                document.getName(), chunkTexts.size(), chunkSize, chunkOverlap);

        int embeddingSuccesses = 0;
        int embeddingFailures = 0;

        // Step 5: Generate embeddings and store chunks
        for (int i = 0; i < chunkTexts.size(); i++) {
            String chunkContent = chunkTexts.get(i);

            // FIX: Use saveAndFlush() to immediately execute the INSERT via JPA before
            // the JDBC UPDATE that writes the embedding vector.
            //
            // Root cause of NULL embeddings:
            // save() only queues the INSERT in JPA's first-level cache — it is NOT sent
            // to the database until the transaction flushes (at commit). The subsequent
            // jdbcTemplate.update() runs via raw JDBC, finds no row with the chunk's UUID,
            // and silently updates 0 rows. saveAndFlush() forces the INSERT to the database
            // within the current transaction so the JDBC UPDATE finds the row.
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunkContent)
                    .chunkIndex(i)
                    .build();
            chunk = chunkRepository.saveAndFlush(chunk);

            log.debug("[Embedding] Requesting embedding for chunk {}/{} of '{}': chunkId={}, textLen={}",
                    i + 1, chunkTexts.size(), document.getName(), chunk.getId(), chunkContent.length());

            try {
                float[] embedding = embeddingService.generateEmbedding(chunkContent);

                log.debug("[Embedding] Received embedding for chunk {}/{}: dimension={}",
                        i + 1, chunkTexts.size(), embedding.length);

                String vectorLiteral = toVectorLiteral(embedding);
                int rows = jdbcTemplate.update(
                        "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
                        vectorLiteral, chunk.getId());

                if (rows == 1) {
                    log.debug("[Embedding] Persisted embedding for chunk id={}: rowsUpdated={}",
                            chunk.getId(), rows);
                    embeddingSuccesses++;
                } else {
                    log.error("[Embedding] FAILED to persist embedding for chunk id={}: rowsUpdated={}. " +
                              "Expected 1 but got {}. The chunk INSERT may not have reached the DB.",
                              chunk.getId(), rows, rows);
                    embeddingFailures++;
                }
            } catch (Exception e) {
                log.error("[Embedding] Exception generating/persisting embedding for chunk {}/{} of '{}': {}",
                        i + 1, chunkTexts.size(), document.getName(), e.getMessage(), e);
                embeddingFailures++;
            }
        }

        log.info("[Embedding] Document '{}': {}/{} embeddings persisted successfully ({} failed)",
                document.getName(), embeddingSuccesses, chunkTexts.size(), embeddingFailures);

        // Step 6: Update document sync timestamp
        document.setLastSync(Instant.now());
        documentRepository.save(document);

        log.info("Finished processing document '{}': {} chunks stored, {} embeddings persisted",
                document.getName(), chunkTexts.size(), embeddingSuccesses);
    }

    private String extractText(byte[] fileContent, String filename) {
        try {
            return tika.parseToString(new ByteArrayInputStream(fileContent));
        } catch (Exception e) {
            log.error("Text extraction failed for file: {}", filename, e);
            return "";
        }
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
}
