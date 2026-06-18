package com.bytehr.service;

import com.bytehr.api.dto.HrChatResponse;
import com.bytehr.api.dto.RelevantChunk;
import com.bytehr.model.Analytics;
import com.bytehr.repository.AnalyticsRepository;
import com.bytehr.service.impl.HrResponseAgentImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrResponseAgentTest {

    @Mock private VectorSearchService vectorSearchService;
    @Mock private ChatService chatService;
    @Mock private LanguageDetectionService languageDetectionService;
    @Mock private ConversationService conversationService;
    @Mock private AnalyticsRepository analyticsRepository;

    private HrResponseAgentImpl agent;

    @BeforeEach
    void setUp() {
        agent = new HrResponseAgentImpl(
                vectorSearchService, chatService,
                languageDetectionService, conversationService, analyticsRepository);
        ReflectionTestUtils.setField(agent, "topK", 5);
        ReflectionTestUtils.setField(agent, "confidenceThreshold", 0.6);
    }

    @Test
    void shouldReturnLowConfidenceMessageWhenNoChunksFound() {
        when(languageDetectionService.detectLanguage(any())).thenReturn("en");
        when(vectorSearchService.search(any(), any(), anyInt())).thenReturn(List.of());
        when(analyticsRepository.save(any())).thenReturn(new Analytics());

        HrChatResponse response = agent.answer(
                "How many vacation days do I have?", "AL", "conv-1", "user-1", "Alice", List.of());

        assertThat(response.getAnswer()).contains("I could not find a reliable answer");
        assertThat(response.isAnswered()).isFalse();
        assertThat(response.getCitations()).isEmpty();
    }

    @Test
    void shouldReturnLowConfidenceMessageWhenScoreBelowThreshold() {
        when(languageDetectionService.detectLanguage(any())).thenReturn("en");
        RelevantChunk lowScore = buildChunk(0.4);
        when(vectorSearchService.search(any(), any(), anyInt())).thenReturn(List.of(lowScore));
        when(analyticsRepository.save(any())).thenReturn(new Analytics());

        HrChatResponse response = agent.answer(
                "How many vacation days do I have?", "AL", "conv-1", "user-1", "Alice", List.of());

        assertThat(response.getAnswer()).contains("I could not find a reliable answer");
        assertThat(response.isAnswered()).isFalse();
    }

    @Test
    void shouldReturnAnswerWhenChunksHaveHighScore() {
        when(languageDetectionService.detectLanguage(any())).thenReturn("en");
        RelevantChunk highScore = buildChunk(0.85);
        when(vectorSearchService.search(any(), any(), anyInt())).thenReturn(List.of(highScore));
        when(chatService.generateAnswer(any())).thenReturn("You have 20 vacation days per year.");
        when(conversationService.saveConversation(any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn(null);
        when(analyticsRepository.save(any())).thenReturn(new Analytics());

        HrChatResponse response = agent.answer(
                "How many vacation days do I have?", "AL", "conv-1", "user-1", "Alice", List.of());

        assertThat(response.getAnswer()).isEqualTo("You have 20 vacation days per year.");
        assertThat(response.isAnswered()).isTrue();
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getConfidenceScore()).isEqualTo(0.85);
    }

    @Test
    void shouldIncludeCitationsFromRetrievedChunks() {
        when(languageDetectionService.detectLanguage(any())).thenReturn("en");
        when(vectorSearchService.search(any(), any(), anyInt()))
                .thenReturn(List.of(buildChunk(0.9), buildChunk(0.8)));
        when(chatService.generateAnswer(any())).thenReturn("Annual leave is 20 days.");
        when(conversationService.saveConversation(any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn(null);
        when(analyticsRepository.save(any())).thenReturn(new Analytics());

        HrChatResponse response = agent.answer(
                "Leave policy", "RS", "conv-2", "user-2", "Bob", List.of());

        assertThat(response.getCitations()).isNotEmpty();
    }

    private RelevantChunk buildChunk(double score) {
        return RelevantChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("HR-Policy-Albania.pdf")
                .sourcePath("https://sharepoint.example.com/hr/HR-Policy-Albania.pdf")
                .content("Employees are entitled to 20 days of annual leave per year.")
                .chunkIndex(0)
                .similarityScore(score)
                .build();
    }
}
