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

        // Step 3: Remove old chunks for this document
        chunkRepository.deleteByDocumentId(document.getId());

        // Step 4: Chunk content
        List<String> chunkTexts = chunkingService.chunk(text, chunkSize, chunkOverlap);
        log.info("Document '{}' split into {} chunks", document.getName(), chunkTexts.size());

        // Step 5: Generate embeddings and store chunks
        for (int i = 0; i < chunkTexts.size(); i++) {
            String chunkContent = chunkTexts.get(i);

            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunkContent)
                    .chunkIndex(i)
                    .build();
            chunkRepository.save(chunk);

            // Generate and store embedding via JDBC (pgvector native type)
            float[] embedding = embeddingService.generateEmbedding(chunkContent);
            String vectorLiteral = toVectorLiteral(embedding);
            jdbcTemplate.update(
                    "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
                    vectorLiteral, chunk.getId());
        }

        // Step 6: Update document sync timestamp
        document.setLastSync(Instant.now());
        documentRepository.save(document);

        log.info("Finished processing document '{}': {} chunks stored", document.getName(), chunkTexts.size());
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
