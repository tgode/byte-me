package com.bytehr.service.impl;

import com.bytehr.api.dto.Citation;
import com.bytehr.api.dto.HrChatResponse;
import com.bytehr.api.dto.RelevantChunk;
import com.bytehr.config.RagProperties;
import com.bytehr.integration.ollama.dto.OllamaMessage;
import com.bytehr.model.Analytics;
import com.bytehr.repository.AnalyticsRepository;
import com.bytehr.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Multilingual retrieval tests — verify that the language detected from the question
 * is correctly passed to the LLM as an explicit language instruction.
 *
 * Covers: EN → English, SQ (Albanian) → Albanian (Shqip), SR (Serbian) → Serbian (Srpski)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultilingualRetrievalTest {

    @Mock private VectorSearchService vectorSearchService;
    @Mock private ChatService chatService;
    @Mock private LanguageDetectionService languageDetectionService;
    @Mock private ConversationService conversationService;
    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private RagProperties ragProperties;

    @InjectMocks
    private HrResponseAgentImpl hrResponseAgent;

    private RelevantChunk vacationChunk;

    @BeforeEach
    void setUp() {
        when(ragProperties.getTopK()).thenReturn(3);
        when(ragProperties.getMaxContextChars()).thenReturn(1500);
        when(ragProperties.isStrictMode()).thenReturn(true);
        when(conversationService.getConversationHistory(anyString(), anyInt())).thenReturn(List.of());
        when(analyticsRepository.save(any(Analytics.class))).thenReturn(new Analytics());

        vacationChunk = RelevantChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("vacation-policy.md")
                .sourcePath("/sample-data/hr-documents/albania/vacation-policy.md")
                .content("Employees with more than 5 years of service receive 25 working days per year.")
                .similarityScore(0.85)
                .chunkIndex(0)
                .build();

        when(vectorSearchService.search(anyString(), any(), anyInt()))
                .thenReturn(List.of(vacationChunk));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Language detection → system prompt injection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void englishQuery_systemPromptContainsEnglishLanguageSpec() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("en");
        when(chatService.generateAnswer(anyList())).thenReturn("You have 25 working days.");

        HrChatResponse response = hrResponseAgent.answer(
                "How many vacation days do I have?", "AL", "conv1", "user1", "Alice", List.of());

        assertThat(response.getDetectedLanguage()).isEqualTo("en");
        assertThat(response.isAnswered()).isTrue();

        ArgumentCaptor<List<OllamaMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatService).generateAnswer(captor.capture());
        String systemContent = captor.getValue().get(0).getContent();
        assertThat(systemContent).contains("English");
        assertThat(systemContent).doesNotContain("Albanian").doesNotContain("Serbian");
    }

    @Test
    void albanianQuery_systemPromptContainsAlbanianLanguageSpec() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("sq");
        when(chatService.generateAnswer(anyList())).thenReturn("Keni 25 ditë pune pushim vjetor.");

        HrChatResponse response = hrResponseAgent.answer(
                "Sa ditë pushimi kam?", "AL", "conv1", "user1", "Arben", List.of());

        assertThat(response.getDetectedLanguage()).isEqualTo("sq");
        assertThat(response.isAnswered()).isTrue();

        ArgumentCaptor<List<OllamaMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatService).generateAnswer(captor.capture());
        String systemContent = captor.getValue().get(0).getContent();
        assertThat(systemContent).contains("Albanian");
        assertThat(systemContent).contains("Shqip");
    }

    @Test
    void serbianQuery_systemPromptContainsSerbianLanguageSpec() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("sr");
        when(chatService.generateAnswer(anyList())).thenReturn("Imate 25 radnih dana godišnjeg odmora.");

        HrChatResponse response = hrResponseAgent.answer(
                "Koliko dana godišnjeg odmora imam?", "RS", "conv1", "user1", "Milica", List.of());

        assertThat(response.getDetectedLanguage()).isEqualTo("sr");
        assertThat(response.isAnswered()).isTrue();

        ArgumentCaptor<List<OllamaMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatService).generateAnswer(captor.capture());
        String systemContent = captor.getValue().get(0).getContent();
        assertThat(systemContent).contains("Serbian");
        assertThat(systemContent).contains("Srpski");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Same question in three languages → same document retrieved
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void sameQuestionInThreeLanguages_allRetrieveSameDocument() {
        when(chatService.generateAnswer(anyList())).thenReturn("25 days.");

        String[] questions = {
            "How many vacation days do I have?",
            "Sa ditë pushimi kam?",
            "Koliko dana godišnjeg odmora imam?"
        };
        String[] langs = {"en", "sq", "sr"};

        for (int i = 0; i < questions.length; i++) {
            when(languageDetectionService.detectLanguage(anyString())).thenReturn(langs[i]);

            HrChatResponse response = hrResponseAgent.answer(
                    questions[i], "AL", "conv" + i, "user" + i, "User", List.of());

            assertThat(response.isAnswered())
                    .as("Language %s should return an answer", langs[i])
                    .isTrue();
            assertThat(response.getCitations())
                    .as("Language %s should cite vacation-policy.md", langs[i])
                    .extracting(Citation::getDocumentName)
                    .contains("vacation-policy.md");
        }

        verify(vectorSearchService, times(3)).search(anyString(), any(), anyInt());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // detectedLanguage propagated to response DTO
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void detectedLanguagePropagatedToResponse_english() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("en");
        when(chatService.generateAnswer(anyList())).thenReturn("Answer.");
        HrChatResponse r = hrResponseAgent.answer("How many days?", null, "c", "u", "U", List.of());
        assertThat(r.getDetectedLanguage()).isEqualTo("en");
    }

    @Test
    void detectedLanguagePropagatedToResponse_albanian() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("sq");
        when(chatService.generateAnswer(anyList())).thenReturn("Përgjigje.");
        HrChatResponse r = hrResponseAgent.answer("Sa ditë?", "AL", "c", "u", "U", List.of());
        assertThat(r.getDetectedLanguage()).isEqualTo("sq");
    }

    @Test
    void detectedLanguagePropagatedToResponse_serbian() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("sr");
        when(chatService.generateAnswer(anyList())).thenReturn("Odgovor.");
        HrChatResponse r = hrResponseAgent.answer("Koliko dana?", "RS", "c", "u", "U", List.of());
        assertThat(r.getDetectedLanguage()).isEqualTo("sr");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unknown language falls back gracefully
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void unknownLanguage_systemPromptContainsFallbackSpec() {
        when(languageDetectionService.detectLanguage(anyString())).thenReturn("de");
        when(chatService.generateAnswer(anyList())).thenReturn("Answer.");

        hrResponseAgent.answer("Wie viele Urlaubstage?", null, "c", "u", "U", List.of());

        ArgumentCaptor<List<OllamaMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatService).generateAnswer(captor.capture());
        assertThat(captor.getValue().get(0).getContent()).contains("de");
    }
}
