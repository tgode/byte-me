package com.bytehr.api;

import com.bytehr.api.dto.ChatRequest;
import com.bytehr.api.dto.Citation;
import com.bytehr.api.dto.HrChatResponse;
import com.bytehr.config.SecurityConfig;
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
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HrResponseAgent hrResponseAgent;

    @MockBean
    private ConversationService conversationService;

    @Test
    void shouldReturn200WithAnswerForValidRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("How many vacation days do I have?")
                .conversationId("conv-123")
                .userId("user-001")
                .country("AL")
                .build();

        HrChatResponse mockResponse = HrChatResponse.builder()
                .answer("You have 20 vacation days per year.")
                .citations(List.of(Citation.builder()
                        .documentName("HR-Policy-Albania.pdf")
                        .pageNumber(3)
                        .build()))
                .confidenceScore(0.92)
                .detectedLanguage("en")
                .answered(true)
                .build();

        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());
        when(hrResponseAgent.answer(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyList()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You have 20 vacation days per year."))
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.confidenceScore").value(0.92))
                .andExpect(jsonPath("$.citations[0].documentName").value("HR-Policy-Albania.pdf"));
    }

    @Test
    void shouldReturn400WhenMessageIsBlank() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WithLowConfidenceResponseWhenNotAnswered() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("What is the weather today?")
                .build();

        HrChatResponse lowConfidence = HrChatResponse.builder()
                .answer("I could not find a reliable answer. Please contact HR.")
                .citations(List.of())
                .confidenceScore(0.0)
                .answered(false)
                .build();

        when(conversationService.getConversationHistory(anyString(), anyInt()))
                .thenReturn(List.of());
        when(hrResponseAgent.answer(anyString(), isNull(), anyString(),
                anyString(), anyString(), anyList()))
                .thenReturn(lowConfidence);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(false))
                .andExpect(jsonPath("$.answer").value(
                        "I could not find a reliable answer. Please contact HR."));
    }
}
