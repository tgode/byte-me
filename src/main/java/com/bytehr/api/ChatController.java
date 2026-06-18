package com.bytehr.api;

import com.bytehr.api.dto.*;
import com.bytehr.service.ConversationService;
import com.bytehr.service.HrResponseAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Direct chat endpoint for MVP testing and hackathon demonstrations.
 * <p>
 * Invokes the same RAG pipeline as the Teams webhook ({@code POST /api/messages})
 * but accepts a plain JSON request — no Bot Framework Activity format required.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Direct RAG question-answering endpoint for MVP testing and demos")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final HrResponseAgent hrResponseAgent;
    private final ConversationService conversationService;

    @PostMapping
    @Operation(
        summary = "Ask an HR question",
        description = """
            Submits a question to the ByteHR RAG pipeline and returns an answer generated
            from indexed HR documents. The same pipeline is used by the Teams bot.

            **Pipeline steps:**
            1. Detect question language (Albanian / Serbian / English)
            2. Generate question embedding (Ollama nomic-embed-text)
            3. Cosine similarity search in pgvector, filtered by country
            4. Build context from top-K document chunks
            5. Generate answer with citations (Ollama qwen3:8b)

            **Low confidence:** When no sufficiently relevant document chunks are found,
            returns `answered: false` and a message to contact HR directly.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Answer generated successfully",
            content = @Content(schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request (blank question, bad country code)",
            content = @Content(schema = @Schema(example = "{\"status\":400,\"error\":\"Bad Request\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal error (Ollama unreachable, DB unavailable)")
    })
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Chat request: question='{}', country='{}', sessionId='{}'",
                request.getQuestion(), request.getCountry(), sessionId);

        List<String> history = conversationService.getConversationHistory(sessionId, 6);

        HrChatResponse hrResponse = hrResponseAgent.answer(
                request.getQuestion(),
                request.getCountry(),
                sessionId,
                "api-user-" + sessionId,
                "API User",
                history
        );

        ChatResponse response = ChatResponse.builder()
                .answer(hrResponse.getAnswer())
                .citations(mapCitations(hrResponse.getCitations()))
                .confidence(hrResponse.getConfidenceScore())
                .detectedLanguage(hrResponse.getDetectedLanguage())
                .answered(hrResponse.isAnswered())
                .build();

        return ResponseEntity.ok(response);
    }

    private List<ChatCitation> mapCitations(List<Citation> citations) {
        if (citations == null) return List.of();
        return citations.stream()
                .map(c -> ChatCitation.builder()
                        .document(c.getDocumentName())
                        .section(c.getSection())
                        .sourcePath(c.getSourcePath())
                        .pageNumber(c.getPageNumber())
                        .build())
                .toList();
    }
}
