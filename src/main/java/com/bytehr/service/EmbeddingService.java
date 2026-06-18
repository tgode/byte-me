package com.bytehr.service;

public interface EmbeddingService {

    /**
     * Generates a float embedding vector for the given text.
     */
    float[] generateEmbedding(String text);
}
