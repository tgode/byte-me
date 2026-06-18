package com.bytehr.service.impl;

import com.bytehr.integration.ollama.OllamaClient;
import com.bytehr.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    /** Expected dimension for nomic-embed-text and the pgvector column definition vector(768). */
    private static final int EXPECTED_DIMENSION = 768;

    private final OllamaClient ollamaClient;

    @Override
    public float[] generateEmbedding(String text) {
        log.debug("[EmbeddingService] Requesting embedding: textLen={}", text.length());

        float[] result = ollamaClient.generateEmbedding(text);

        log.debug("[EmbeddingService] Received embedding: dimension={}", result.length);

        if (result.length != EXPECTED_DIMENSION) {
            log.warn("[EmbeddingService] Unexpected embedding dimension: {} (expected {} for nomic-embed-text). " +
                     "pgvector column is vector({}). Dimension mismatch will cause INSERT/UPDATE to fail.",
                     result.length, EXPECTED_DIMENSION, EXPECTED_DIMENSION);
        }

        return result;
    }
}
