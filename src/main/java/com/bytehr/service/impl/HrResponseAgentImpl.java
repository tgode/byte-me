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

    /** The exact phrase the LLM emits when the context doesn't contain the answer. */
    private static final String LLM_FALLBACK_PHRASE = "I could not find this information in the HR documents.";

    /**
     * Standard system prompt — used when strict-mode=false.
     * %1$s = document context, %2$s = language specification
     */
    private static final String SYSTEM_PROMPT_STANDARD = """
            You are ByteHR AI, an HR assistant for employees in Albania and Serbia.

            RULES:
            1. Answer ONLY HR-related questions using the provided document context.
            2. NEVER invent, hallucinate, or assume company policies not present in the context.
            3. If the question is not HR-related, respond: "This question is outside my HR scope. Can I help you with vacation, leave, benefits, or other HR topics?"
            4. Always respond in the SAME LANGUAGE as the user's question.
            5. Always include citations referencing the document name and relevant section.
            6. Keep responses clear and concise.
            7. Country-specific policies from the user's country take priority.

            CONTEXT FROM HR DOCUMENTS:
            %1$s

            Respond ONLY in %2$s. Do not switch languages.
            """;

    /**
     * Strict system prompt — used when strict-mode=true (default).
     * %1$s = document context, %2$s = language specification
     */
    private static final String SYSTEM_PROMPT_STRICT = """
            You are ByteHR AI, an HR assistant for employees in Albania and Serbia.

            STRICT RAG MODE — MANDATORY RULES (follow exactly):

            1. Answer ONLY using the document context provided below. No exceptions.
            2. Do NOT invent, estimate, or assume any information not explicitly stated in the context.
            3. Do NOT add HR contact details, email addresses, phone numbers, or URLs unless they appear word-for-word in the context.
            4. Do NOT mention carry-over policies, deadlines, benefits, bonuses, or procedures that are not explicitly written in the context.
            5. If the answer to the question is NOT found in the context, respond with this exact phrase:
               "I could not find this information in the HR documents."
            6. If the question is not HR-related, respond:
               "This question is outside my HR scope. Can I help you with vacation, leave, benefits, or other HR topics?"
            7. When you provide an answer, quote or closely paraphrase the relevant text from the context and name the source document.
            8. Country-specific policies from the user's country take priority over global policies.
            9. Keep responses concise and factual. Do not add qualifications, suggestions, or advice beyond what the documents state.

            CONTEXT FROM HR DOCUMENTS:
            %1$s

            CRITICAL: Use ONLY the above context. Do not supplement with general knowledge.
            Respond ONLY in %2$s. Do not switch languages mid-answer.
            """;

    /** Keywords that signal an EPR/goal-setting query — same set used by VectorSearchServiceImpl. */
    private static final Set<String> EPR_QUERY_KEYWORDS = Set.of(
            "goal", "goals", "objective", "objectives", "performance", "epr",
            "mid-year", "midyear", "review", "appraisal", "evaluation",
            "cilj", "ciljeve", "ciljevi", "performanse", "pregled", "procena", "ocena",
            "qëllim", "qëllime", "performancë", "vlerësim", "objektiv"
    );

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
        String languageSpec = buildLanguageSpec(detectedLanguage);

        // For EPR/goal queries, expand the context window to capture longer PPTX FAQ slides
        boolean isEprQuery = isEprRelatedQuery(question);
        int effectiveMaxContextChars = isEprQuery
                ? Math.max(ragProperties.getMaxContextChars(), 3000)
                : ragProperties.getMaxContextChars();

        log.info("[RAG] Query: user='{}', country='{}', detectedLanguage='{}', lang='{}', topK={}, " +
                 "maxContextChars={}, strictMode={}, eprQuery={}",
                userId, country, detectedLanguage, languageSpec,
                ragProperties.getTopK(), effectiveMaxContextChars,
                ragProperties.isStrictMode(), isEprQuery);

        // Retrieve relevant chunks using configurable topK
        List<RelevantChunk> chunks = vectorSearchService.search(question, country, ragProperties.getTopK());

        // ── [RAG Validation] block ────────────────────────────────────────────
        if (chunks.isEmpty()) {
            log.info("[RAG Validation] retrievedChunks=0 contextChars=0 " +
                     "fallbackTriggered=true fallbackReason=NO_CHUNKS_RETRIEVED");
            return buildLowConfidenceResponse(detectedLanguage, startTime, userId, null, false);
        }

        double maxScore = chunks.stream().mapToDouble(RelevantChunk::getSimilarityScore).max().orElse(0.0);

        // Log each retrieved chunk for diagnostics
        log.info("[RAG Validation] retrievedChunks={}:", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            RelevantChunk chunk = chunks.get(i);
            log.info("[RAG Validation]   #{}  document='{}' score={} chunkIdx={} " +
                     "contentLen={}  contentPreview={}",
                    i + 1,
                    chunk.getDocumentName(),
                    String.format("%.4f", chunk.getSimilarityScore()),
                    chunk.getChunkIndex(),
                    chunk.getContent().length(),
                    repr(chunk.getContent(), 120));
        }

        if (maxScore < confidenceThreshold) {
            log.info("[RAG Validation] fallbackTriggered=true fallbackReason=LOW_CONFIDENCE " +
                     "maxScore={} threshold={}", maxScore, confidenceThreshold);
            return buildLowConfidenceResponse(detectedLanguage, startTime, userId, null, false);
        }

        // Build context with effective character limit
        String context = buildContext(chunks, effectiveMaxContextChars);
        int totalPromptChars = systemPromptTemplate(ragProperties.isStrictMode())
                .formatted(context, languageSpec).length();
        int estimatedTokens = totalPromptChars / 4;

        log.info("[RAG Validation] contextChars={} estimatedTokens=~{} fallbackTriggered=false " +
                 "maxScore={}", context.length(), estimatedTokens, maxScore);

        // Log first 1000 chars of generated context for debugging
        log.info("[RAG Validation] contextPreview={}",
                repr(context, 1000));

        // Select prompt template and inject context + explicit language
        String systemPrompt = String.format(
                ragProperties.isStrictMode() ? SYSTEM_PROMPT_STRICT : SYSTEM_PROMPT_STANDARD,
                context, languageSpec);

        // Build message list: system + history + current question
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(OllamaMessage.builder().role("system").content(systemPrompt).build());

        List<String> recentHistory = conversationHistory.size() > 12
                ? conversationHistory.subList(conversationHistory.size() - 12, conversationHistory.size())
                : conversationHistory;
        for (int i = 0; i < recentHistory.size() - 1; i += 2) {
            messages.add(OllamaMessage.builder().role("user").content(recentHistory.get(i)).build());
            messages.add(OllamaMessage.builder().role("assistant").content(recentHistory.get(i + 1)).build());
        }

        messages.add(OllamaMessage.builder().role("user").content(question).build());

        // Prompt stats before LLM call
        log.debug("[RAG] Prompt stats: messages={}, totalChars={}, estimatedTokens=~{}, historyTurns={}",
                messages.size(), totalPromptChars, estimatedTokens, recentHistory.size() / 2);

        String rawAnswer = chatService.generateAnswer(messages);
        List<Citation> citations = buildCitations(chunks);

        // Detect LLM-generated fallback (vs code-level fallback)
        boolean llmFallback = rawAnswer.contains(LLM_FALLBACK_PHRASE)
                || rawAnswer.toLowerCase().contains("could not find this information");

        log.info("[RAG Validation] answerLen={} llmFallbackDetected={}", rawAnswer.length(), llmFallback);

        if (llmFallback) {
            log.warn("[RAG Validation] LLM returned fallback despite {} retrieved chunks " +
                     "(contextChars={}, maxScore={}). " +
                     "Context may be too sparse or irrelevant for the question.",
                    chunks.size(), context.length(), maxScore);
        }

        long responseTimeMs = System.currentTimeMillis() - startTime;
        log.info("[RAG] Answered in {}ms: confidence={}, chunks={}, citations={}, lang='{}', strictMode={}",
                responseTimeMs, String.format("%.3f", maxScore),
                chunks.size(), citations.size(), detectedLanguage, ragProperties.isStrictMode());

        // Persist conversation turn
        conversationService.saveConversation(conversationId, userId, userName, country, question, rawAnswer, maxScore);

        // Record analytics
        recordAnalytics(null, responseTimeMs, maxScore, detectedLanguage, country, !llmFallback);

        return HrChatResponse.builder()
                .answer(rawAnswer)
                .citations(citations)
                .confidenceScore(maxScore)
                .detectedLanguage(detectedLanguage)
                .answered(!llmFallback)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Context building
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a context string from retrieved chunks, capped at maxContextChars total.
     * Overflowing chunks are truncated. EPR queries use an expanded limit.
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

        return sb.toString();
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

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isEprRelatedQuery(String question) {
        String lower = question.toLowerCase();
        return EPR_QUERY_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String systemPromptTemplate(boolean strict) {
        return strict ? SYSTEM_PROMPT_STRICT : SYSTEM_PROMPT_STANDARD;
    }

    private String buildLanguageSpec(String langCode) {
        return switch (langCode.toLowerCase()) {
            case "sq" -> "Albanian (Shqip)";
            case "sr" -> "Serbian (Srpski)";
            case "en" -> "English";
            default   -> "the same language as the user's question (detected: " + langCode + ")";
        };
    }

    /** Returns a repr-style preview: up to maxLen chars, escaped for log readability. */
    private String repr(String text, int maxLen) {
        if (text == null) return "null";
        String truncated = text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
        return truncated.replace("\n", "\\n").replace("\r", "");
    }

    private List<Citation> buildCitations(List<RelevantChunk> chunks) {
        List<Citation> citations = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RelevantChunk chunk : chunks) {
            String key = chunk.getDocumentId().toString();
            if (seen.add(key)) {
                citations.add(Citation.builder()
                        .documentId(chunk.getDocumentId())
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
