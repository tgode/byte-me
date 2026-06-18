package com.bytehr.service.impl;

import com.bytehr.integration.ollama.OllamaClient;
import com.bytehr.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final OllamaClient ollamaClient;

    @Override
    public float[] generateEmbedding(String text) {
        return ollamaClient.generateEmbedding(text);
    }
}
