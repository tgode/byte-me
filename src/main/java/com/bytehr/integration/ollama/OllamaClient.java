package com.bytehr.integration.ollama;

import com.bytehr.integration.ollama.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class OllamaClient {

    private final WebClient webClient;
    private final String embeddingModel;
    private final String chatModel;
    private final long timeoutSeconds;

    public OllamaClient(
            WebClient.Builder webClientBuilder,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.embedding-model}") String embeddingModel,
            @Value("${ollama.chat-model}") String chatModel,
            @Value("${ollama.timeout-seconds}") long timeoutSeconds) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Generates an embedding vector for the given text using nomic-embed-text.
     */
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text of length {}", text.length());

        OllamaEmbeddingRequest request = OllamaEmbeddingRequest.builder()
                .model(embeddingModel)
                .input(text)
                .build();

        OllamaEmbeddingResponse response = webClient.post()
                .uri("/api/embed")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).map(body -> {
                            log.error("Ollama /api/embed error: HTTP {} — {}", clientResponse.statusCode(), body);
                            return new IllegalStateException("Ollama embedding failed: " + body);
                        }))
                .bodyToMono(OllamaEmbeddingResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
            throw new IllegalStateException("Ollama returned empty embedding for model: " + embeddingModel);
        }

        List<Float> embedding = response.getEmbeddings().get(0);
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i);
        }
        return result;
    }

    /**
     * Generates an answer using qwen3:8b given a list of chat messages.
     */
    public String generateAnswer(List<OllamaMessage> messages) {
        log.debug("Generating chat answer with {} messages", messages.size());

        OllamaChatRequest request = OllamaChatRequest.builder()
                .model(chatModel)
                .messages(messages)
                .stream(false)
                .options(OllamaChatOptions.builder().temperature(0.1).numCtx(4096).build())
                .build();

        OllamaChatResponse response = webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response == null || response.getMessage() == null) {
            throw new IllegalStateException("Ollama returned empty chat response");
        }
        return response.getMessage().getContent();
    }
}
