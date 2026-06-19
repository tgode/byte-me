package com.bytehr.api;

import com.bytehr.api.dto.*;
import com.bytehr.service.ConversationService;
import com.bytehr.service.HrResponseAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(com.bytehr.config.SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HrResponseAgent hrResponseAgent;

    @MockBean
    private ConversationService conversationService;

    // ────────────────────────────────────────────────────────────────────────
    // Happy path tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void chat_validQuestion_returns200WithAnswer() throws Exception {
        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());

        HrChatResponse agentResponse = HrChatResponse.builder()
                .answer("You are entitled to 20 working days of annual vacation.")
                .citations(List.of(Citation.builder()
                        .documentName("vacation-policy.md")
                        .sourcePath("/sample-data/hr-documents/albania/vacation-policy.md")
                        .build()))
                .confidenceScore(0.92)
                .detectedLanguage("en")
                .answered(true)
                .build();

        when(hrResponseAgent.answer(anyString(), any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(agentResponse);

        ChatRequest request = ChatRequest.builder()
                .message("How many vacation days do I have?")
                .country("AL")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You are entitled to 20 working days of annual vacation."))
                .andExpect(jsonPath("$.confidence").value(0.92))
                .andExpect(jsonPath("$.detectedLanguage").value("en"))
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.citations[0].document").value("vacation-policy.md"));
    }

    @Test
    void chat_noCountry_returns200() throws Exception {
        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());
        when(hrResponseAgent.answer(anyString(), isNull(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(HrChatResponse.builder()
                        .answer("Policy answer.")
                        .citations(List.of())
                        .confidenceScore(0.85)
                        .detectedLanguage("en")
                        .answered(true)
                        .build());

        ChatRequest request = ChatRequest.builder()
                .message("What is the remote work policy?")
                .build(); // no country

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(true));
    }

    @Test
    void chat_withSessionId_usesProvidedSession() throws Exception {
        String sessionId = "my-session-001";
        when(conversationService.getConversationHistory(eq(sessionId), anyInt()))
                .thenReturn(List.of("Previous question", "Previous answer"));
        when(hrResponseAgent.answer(anyString(), any(), eq(sessionId), anyString(), anyString(), anyList()))
                .thenReturn(HrChatResponse.builder()
                        .answer("Follow-up answer.")
                        .citations(List.of())
                        .confidenceScore(0.88)
                        .detectedLanguage("en")
                        .answered(true)
                        .build());

        ChatRequest request = ChatRequest.builder()
                .message("And what about Serbia?")
                .conversationId(sessionId)
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Follow-up answer."));
    }

    @Test
    void chat_lowConfidence_returns200WithFallbackMessage() throws Exception {
        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());
        when(hrResponseAgent.answer(anyString(), any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(HrChatResponse.builder()
                        .answer("I could not find a reliable answer. Please contact HR.")
                        .citations(List.of())
                        .confidenceScore(0.0)
                        .detectedLanguage("en")
                        .answered(false)
                        .build());

        ChatRequest request = ChatRequest.builder()
                .message("What is the weather today?")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(false))
                .andExpect(jsonPath("$.confidence").value(0.0))
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test
    void chat_multipleCitations_allMapped() throws Exception {
        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());
        when(hrResponseAgent.answer(anyString(), any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(HrChatResponse.builder()
                        .answer("Benefits include health insurance and gym membership.")
                        .citations(List.of(
                                Citation.builder().documentName("employee-benefits.md").sourcePath("/doc1").build(),
                                Citation.builder().documentName("remote-work-policy.md").sourcePath("/doc2").build()
                        ))
                        .confidenceScore(0.90)
                        .detectedLanguage("en")
                        .answered(true)
                        .build());

        ChatRequest request = ChatRequest.builder()
                .message("What benefits does the company offer?")
                .country("RS")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations.length()").value(2))
                .andExpect(jsonPath("$.citations[0].document").value("employee-benefits.md"))
                .andExpect(jsonPath("$.citations[1].document").value("remote-work-policy.md"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Validation tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void chat_blankQuestion_returns400() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_nullQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"AL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_invalidCountryCode_returns400() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("How many vacation days?")
                .country("INVALID")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_questionTooLong_returns400() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("A".repeat(2001))
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
