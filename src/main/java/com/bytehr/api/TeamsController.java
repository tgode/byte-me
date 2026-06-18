package com.bytehr.api;

import com.bytehr.api.dto.*;
import com.bytehr.service.ConversationService;
import com.bytehr.service.HrResponseAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Teams Bot endpoint — receives Bot Framework Activities from Microsoft Teams.
 * <p>
 * Teams sends POST requests to /api/messages for every user interaction.
 * This controller processes message activities and replies using the HR Response Agent.
 */
@RestController
@RequestMapping("/api/messages")
@Slf4j
@RequiredArgsConstructor
public class TeamsController {

    private final HrResponseAgent hrResponseAgent;
    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<TeamsActivityResponse> handleActivity(@RequestBody TeamsActivity activity) {
        log.debug("Received Teams activity: type={}, from={}", activity.getType(), activity.getFrom());

        if (!"message".equalsIgnoreCase(activity.getType())) {
            // Acknowledge non-message activities (e.g., conversationUpdate)
            return ResponseEntity.ok(TeamsActivityResponse.builder().type("invoke").text("").build());
        }

        String question = activity.getText();
        if (question == null || question.isBlank()) {
            return ResponseEntity.ok(TeamsActivityResponse.builder()
                    .text("Hello! I am ByteHR AI. Ask me any HR-related question.")
                    .build());
        }

        String userId = extractUserId(activity);
        String userName = activity.getFrom() != null ? activity.getFrom().getName() : "Employee";
        String conversationId = activity.getConversation() != null
                ? activity.getConversation().getId() : userId;

        // Resolve country from Teams user context (AAD objectId is available; country derived from profile)
        // In production this is enriched via Microsoft Graph. For MVP, default to null (no filtering).
        String country = resolveCountry(activity);

        // Retrieve conversation history for context
        List<String> history = conversationService.getConversationHistory(conversationId, 6);

        HrChatResponse hrResponse = hrResponseAgent.answer(
                question, country, conversationId, userId, userName, history);

        String formattedReply = formatReply(hrResponse);

        return ResponseEntity.ok(TeamsActivityResponse.builder()
                .type("message")
                .text(formattedReply)
                .textFormat("markdown")
                .build());
    }

    private String extractUserId(TeamsActivity activity) {
        if (activity.getFrom() != null) {
            return activity.getFrom().getAadObjectId() != null
                    ? activity.getFrom().getAadObjectId()
                    : activity.getFrom().getId();
        }
        return "unknown";
    }

    /**
     * Resolves the user's country from Teams context.
     * In a production setup this would call Microsoft Graph /me to get the user's UsageLocation.
     * For the MVP, we detect from the Teams tenant configuration or default to null.
     */
    private String resolveCountry(TeamsActivity activity) {
        // Placeholder: country can be enriched via Graph API call based on AAD user profile.
        // The VectorSearchService gracefully handles null country by searching all documents.
        return null;
    }

    private String formatReply(HrChatResponse response) {
        StringBuilder sb = new StringBuilder(response.getAnswer());

        if (!response.getCitations().isEmpty()) {
            sb.append("\n\n---\n**Sources:**\n");
            for (Citation citation : response.getCitations()) {
                sb.append("- **").append(citation.getDocumentName()).append("**");
                if (citation.getPageNumber() != null) {
                    sb.append(" (Page ").append(citation.getPageNumber()).append(")");
                }
                // Only show link for SharePoint documents (webUrl) — never expose filesystem paths
                if (citation.getWebUrl() != null && !citation.getWebUrl().isBlank()) {
                    sb.append(" — [View document](").append(citation.getWebUrl()).append(")");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
