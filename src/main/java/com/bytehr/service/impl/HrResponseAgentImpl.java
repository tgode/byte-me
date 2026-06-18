package com.bytehr.service.impl;

import com.bytehr.api.dto.Citation;
import com.bytehr.api.dto.HrChatResponse;
import com.bytehr.api.dto.RelevantChunk;
import com.bytehr.config.RagProperties;
import com.bytehr.integration.ollama.dto.OllamaMessage;
import com.bytehr.model.Analytics;
import com.bytehr.repository.AnalyticsRepository;
import com.bytehr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrResponseAgentImpl implements HrResponseAgent {

    private static final String LOW_CONFIDENCE_RESPONSE =
            "I could not find a reliable answer. Please contact HR.";

    private static final String SYSTEM_PROMPT = """
            You are ByteHR AI, an HR assistant for employees in Albania and Serbia.
            
            STRICT RULES:
            1. Answer ONLY HR-related questions using the provided document context.
            2. NEVER invent, hallucinate, or assume company policies not present in the context.
            3. If the question is not HR-related, respond: "This question is outside my HR scope. Can I help you with vacation, leave, benefits, or other HR topics?"
            4. Always respond in the SAME LANGUAGE as the user's question.
            5. Always include citations referencing the document name and relevant section.
            6. Keep responses clear and concise.
            7. Country-specific policies from the user's country take priority.
            
            CONTEXT FROM HR DOCUMENTS:
            %s
            
            Respond in the language the user used.
            """;

    private final VectorSearchService vectorSearchService;
    private final ChatService chatService;
    private final LanguageDetectionService languageDetectionService;
    private final ConversationService conversationService;
    private final AnalyticsRepository analyticsRepository;
    private final RagProperties ragProperties;

    @Value("${vector-search.confidence-threshold}")
    private double confidenceThreshold;

    @Override
    public HrChatResponse answer(String question, String country, String conversationId,
                                 String userId, String userName, List<String> conversationHistory) {
        long startTime = System.currentTimeMillis();
        String detectedLanguage = languageDetectionService.detectLanguage(question);
        log.info("[RAG] Query: user='{}', country='{}', lang='{}', topK={}, maxContextChars={}",
                userId, country, detectedLanguage, ragProperties.getTopK(), ragProperties.getMaxContextChars());

        // Retrieve relevant chunks using configurable topK
        List<RelevantChunk> chunks = vectorSearchService.search(question, country, ragProperties.getTopK());

        if (chunks.isEmpty()) {
            log.info("[RAG] No chunks found — low confidence response");
            return buildLowConfidenceResponse(detectedLanguage, startTime, userId, null, false);
        }

        double maxScore = chunks.stream().mapToDouble(RelevantChunk::getSimilarityScore).max().orElse(0.0);

        if (maxScore < confidenceThreshold) {
            log.info("[RAG] Confidence too low: maxScore={} < threshold={}", maxScore, confidenceThreshold);
            return buildLowConfidenceResponse(detectedLanguage, startTime, userId, null, false);
        }

        // Build context with character limit to control prompt size
        String context = buildContext(chunks, ragProperties.getMaxContextChars());
        String systemPrompt = String.format(SYSTEM_PROMPT, context);

        // Build message list: system + history + current question
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(OllamaMessage.builder().role("system").content(systemPrompt).build());

        // Add conversation history (up to last 6 exchanges = 12 strings)
        List<String> recentHistory = conversationHistory.size() > 12
                ? conversationHistory.subList(conversationHistory.size() - 12, conversationHistory.size())
                : conversationHistory;
        for (int i = 0; i < recentHistory.size() - 1; i += 2) {
            messages.add(OllamaMessage.builder().role("user").content(recentHistory.get(i)).build());
            messages.add(OllamaMessage.builder().role("assistant").content(recentHistory.get(i + 1)).build());
        }

        messages.add(OllamaMessage.builder().role("user").content(question).build());

        // Log prompt statistics before sending to LLM
        int totalPromptChars = messages.stream().mapToInt(m -> m.getContent().length()).sum();
        // Rough estimation: ~4 chars/token (averaged across English, Albanian, Serbian)
        int estimatedTokens = totalPromptChars / 4;
        log.debug("[RAG] Prompt stats: messages={}, totalChars={}, estimatedTokens=~{}, " +
                  "contextChars={}, historyTurns={}",
                messages.size(), totalPromptChars, estimatedTokens,
                context.length(), recentHistory.size() / 2);

        String rawAnswer = chatService.generateAnswer(messages);
        List<Citation> citations = buildCitations(chunks);

        long responseTimeMs = System.currentTimeMillis() - startTime;
        log.info("[RAG] Answered in {}ms: confidence={}, chunks={}, citations={}",
                responseTimeMs, String.format("%.3f", maxScore), chunks.size(), citations.size());

        // Persist conversation turn
        conversationService.saveConversation(conversationId, userId, userName, country, question, rawAnswer, maxScore);

        // Record analytics
        recordAnalytics(null, responseTimeMs, maxScore, detectedLanguage, country, true);

        return HrChatResponse.builder()
                .answer(rawAnswer)
                .citations(citations)
                .confidenceScore(maxScore)
                .detectedLanguage(detectedLanguage)
                .answered(true)
                .build();
    }

    /**
     * Builds a context string from retrieved chunks, capped at maxContextChars total.
     * Chunks are included in relevance order. A chunk that overflows the budget is truncated.
     */
    private String buildContext(List<RelevantChunk> chunks, int maxContextChars) {
        StringBuilder sb = new StringBuilder();
        int remaining = maxContextChars;

        for (int i = 0; i < chunks.size(); i++) {
            if (remaining <= 0) break;

            RelevantChunk chunk = chunks.get(i);
            String header = buildChunkHeader(chunk, i + 1);

            if (header.length() >= remaining) break;
            remaining -= header.length();

            String content = chunk.getContent();
            if (content.length() > remaining) {
                int cutAt = Math.max(0, remaining - 3);
                log.debug("[RAG] Chunk {} truncated: {} -> {} chars", i + 1, content.length(), cutAt);
                content = content.substring(0, cutAt) + "...";
            }

            sb.append(header).append(content).append("\n\n");
            remaining -= content.length() + 2;
        }

        String context = sb.toString();
        log.debug("[RAG] Context: {}/{} chars used", context.length(), maxContextChars);
        return context;
    }

    private String buildChunkHeader(RelevantChunk chunk, int number) {
        StringBuilder header = new StringBuilder("--- Source ")
                .append(number).append(": ").append(chunk.getDocumentName());
        if (chunk.getPageNumber() != null) {
            header.append(" (Page ").append(chunk.getPageNumber()).append(")");
        }
        header.append(" ---\n");
        return header.toString();
    }

    private List<Citation> buildCitations(List<RelevantChunk> chunks) {
        List<Citation> citations = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RelevantChunk chunk : chunks) {
            String key = chunk.getDocumentId().toString();
            if (seen.add(key)) {
                citations.add(Citation.builder()
                        .documentName(chunk.getDocumentName())
                        .sourcePath(chunk.getSourcePath())
                        .pageNumber(chunk.getPageNumber())
                        .build());
            }
        }
        return citations;
    }

    private HrChatResponse buildLowConfidenceResponse(String language, long startTime,
                                                       String userId, UUID conversationId, boolean answered) {
        recordAnalytics(conversationId, System.currentTimeMillis() - startTime, 0.0, language, null, answered);
        return HrChatResponse.builder()
                .answer(LOW_CONFIDENCE_RESPONSE)
                .citations(List.of())
                .confidenceScore(0.0)
                .detectedLanguage(language)
                .answered(false)
                .build();
    }

    private void recordAnalytics(UUID conversationId, long responseTimeMs, double confidence,
                                  String language, String country, boolean answered) {
        try {
            analyticsRepository.save(Analytics.builder()
                    .conversationId(conversationId)
                    .responseTimeMs(responseTimeMs)
                    .confidenceScore(BigDecimal.valueOf(confidence))
                    .questionLanguage(language)
                    .userCountry(country)
                    .wasAnswered(answered)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record analytics", e);
        }
    }
}
