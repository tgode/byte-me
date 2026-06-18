package com.bytehr.api;

import com.bytehr.api.dto.ChatRequest;
import com.bytehr.api.dto.HrChatResponse;
import com.bytehr.service.ConversationService;
import com.bytehr.service.HrResponseAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Primary REST API endpoint for the Angular frontend and direct API consumers.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "HR question-answering endpoint (RAG pipeline)")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final HrResponseAgent hrResponseAgent;
    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Ask an HR question",
        description = "Submits a question through the RAG pipeline. Returns an answer generated " +
                      "from indexed HR documents with citations and a confidence score.")
    public ResponseEntity<HrChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request from userId='{}', country='{}'", request.getUserId(), request.getCountry());

        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        String userId   = request.getUserId()   != null ? request.getUserId()   : "anonymous";
        String userName = request.getUserName() != null ? request.getUserName() : "Employee";

        List<String> history = conversationService.getConversationHistory(conversationId, 6);

        HrChatResponse response = hrResponseAgent.answer(
                request.getMessage(),
                request.getCountry(),
                conversationId,
                userId,
                userName,
                history);

        return ResponseEntity.ok(response);
    }
}
