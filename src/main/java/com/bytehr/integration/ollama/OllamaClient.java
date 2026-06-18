package com.bytehr.integration.ollama;

import com.bytehr.integration.ollama.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class OllamaClient {

    private static final String EMBED_PATH = "/api/embed";
    private static final String CHAT_PATH  = "/api/chat";

    private final WebClient webClient;
    private final String baseUrl;
    private final String embeddingModel;
    private final String chatModel;
    private final long embeddingTimeoutSeconds;
    private final long chatTimeoutSeconds;

    public OllamaClient(
            WebClient.Builder webClientBuilder,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.embedding-model}") String embeddingModel,
            @Value("${ollama.chat-model}") String chatModel,
            @Value("${ollama.embedding-timeout-seconds:${ollama.timeout-seconds:120}}") long embeddingTimeoutSeconds,
            @Value("${ollama.chat-timeout-seconds:${ollama.timeout-seconds:300}}") long chatTimeoutSeconds) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.baseUrl = baseUrl;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.embeddingTimeoutSeconds = embeddingTimeoutSeconds;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    /**
     * Generates a 768-dimensional embedding vector using POST /api/embed (Ollama current API).
     * Replaces the deprecated /api/embeddings endpoint (which used "prompt" field and
     * returned {"embedding": [...]}) with the current API (which uses "input" and returns
     * {"embeddings": [[...]]}).
     */
    public float[] generateEmbedding(String text) {
        long start = System.currentTimeMillis();
        log.debug("[OllamaClient] Embed request: url={}{}, model='{}', textLen={}, timeout={}s",
                baseUrl, EMBED_PATH, embeddingModel, text.length(), embeddingTimeoutSeconds);

        OllamaEmbedRequest request = OllamaEmbedRequest.builder()
                .model(embeddingModel)
                .input(text)
                .build();

        try {
            OllamaEmbedResponse response = webClient.post()
                    .uri(EMBED_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(body -> new IllegalStateException(
                                String.format("Ollama %s error %s: %s",
                                    EMBED_PATH, clientResponse.statusCode(), body)))
                    )
                    .bodyToMono(OllamaEmbedResponse.class)
                    .timeout(Duration.ofSeconds(embeddingTimeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
                log.error("[OllamaClient] Empty embedding response: url={}{}, model='{}', elapsed={}ms",
                        baseUrl, EMBED_PATH, embeddingModel, elapsed);
                throw new IllegalStateException("Ollama returned empty embeddings for model: " + embeddingModel);
            }

            List<Float> embedding = response.getEmbeddings().get(0);
            long totalMs = response.getTotalDuration() != null ? response.getTotalDuration() / 1_000_000 : -1;

            log.debug("[OllamaClient] Embed response: model='{}', dim={}, totalDuration={}ms, elapsed={}ms",
                    embeddingModel, embedding.size(), totalMs, elapsed);

            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i);
            }
            return result;

        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[OllamaClient] Embed HTTP error: url={}{}, status={}, body='{}', elapsed={}ms",
                    baseUrl, EMBED_PATH, e.getStatusCode(), e.getResponseBodyAsString(), elapsed);
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[OllamaClient] Embed failed: url={}{}, model='{}', elapsed={}ms, error={}",
                    baseUrl, EMBED_PATH, embeddingModel, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * Generates an HR answer using POST /api/chat with thinking mode disabled for lower latency.
     * qwen3:1.7b with think:false; think=false disables chain-of-thought for lower latency.
     */
    public String generateAnswer(List<OllamaMessage> messages) {
        long start = System.currentTimeMillis();

        // Structured performance logging — emitted before and after every generation call
        int promptChars = messages.stream().mapToInt(m -> m.getContent().length()).sum();
        int estimatedTokens = promptChars / 4; // ~4 chars/token across EN/SQ/SR

        log.info("[Chat] model={}", chatModel);
        log.info("[Chat] promptChars={}", promptChars);
        log.info("[Chat] estimatedTokens={}", estimatedTokens);
        log.debug("[OllamaClient] Chat request: url={}{}, model='{}', messages={}, timeout={}s",
                baseUrl, CHAT_PATH, chatModel, messages.size(), chatTimeoutSeconds);

        OllamaChatRequest request = OllamaChatRequest.builder()
                .model(chatModel)
                .messages(messages)
                .stream(false)
                .options(OllamaChatOptions.builder()
                        .temperature(0.1)
                        .numCtx(4096)
                        .think(false)
                        .build())
                .build();

        try {
            OllamaChatResponse response = webClient.post()
                    .uri(CHAT_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(body -> new IllegalStateException(
                                String.format("Ollama %s error %s: %s",
                                    CHAT_PATH, clientResponse.statusCode(), body)))
                    )
                    .bodyToMono(OllamaChatResponse.class)
                    .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            if (response == null || response.getMessage() == null) {
                log.error("[OllamaClient] Empty chat response: url={}{}, model='{}', elapsed={}ms",
                        baseUrl, CHAT_PATH, chatModel, elapsed);
                throw new IllegalStateException("Ollama returned empty chat response for model: " + chatModel);
            }

            String content = response.getMessage().getContent();
            log.info("[Chat] generationDurationMs={}", elapsed);
            log.debug("[OllamaClient] Chat response: model='{}', responseLen={}, elapsed={}ms",
                    chatModel, content.length(), elapsed);

            return content;

        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[OllamaClient] Chat HTTP error: url={}{}, status={}, body='{}', elapsed={}ms",
                    baseUrl, CHAT_PATH, e.getStatusCode(), e.getResponseBodyAsString(), elapsed);
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[OllamaClient] Chat failed: url={}{}, model='{}', elapsed={}ms, error={}",
                    baseUrl, CHAT_PATH, chatModel, elapsed, e.getMessage());
            throw e;
        }
    }
}
