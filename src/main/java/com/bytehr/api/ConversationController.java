package com.bytehr.api;

import com.bytehr.api.dto.ConversationDto;
import com.bytehr.model.Conversation;
import com.bytehr.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoint for retrieving conversation history.
 *
 * GET /api/conversations/{id} — get all turns for a conversation.
 */
@RestController
@RequestMapping("/api/conversations")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationRepository conversationRepository;

    @GetMapping("/{id}")
    public ResponseEntity<List<ConversationDto>> getConversation(@PathVariable String id) {
        log.debug("Fetching conversation history for id={}", id);
        List<Conversation> turns =
                conversationRepository.findByTeamsConversationIdOrderByTimestampAsc(id);

        List<ConversationDto> dtos = turns.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDto>> getUserConversations(@PathVariable String userId) {
        log.debug("Fetching conversations for userId={}", userId);
        List<Conversation> turns =
                conversationRepository.findByUserIdOrderByTimestampDesc(userId);

        List<ConversationDto> dtos = turns.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    private ConversationDto toDto(Conversation c) {
        return ConversationDto.builder()
                .id(c.getId())
                .teamsConversationId(c.getTeamsConversationId())
                .userId(c.getUserId())
                .userName(c.getUserName())
                .userCountry(c.getUserCountry())
                .question(c.getQuestion())
                .response(c.getResponse())
                .confidenceScore(c.getConfidenceScore())
                .timestamp(c.getTimestamp())
                .build();
    }
}
