package com.bytehr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HR question request")
public class ChatRequest {

    @NotBlank(message = "message must not be blank")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    @Schema(description = "The HR question to answer",
            example = "How many vacation days do I have?",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Conversation ID for multi-turn context. Auto-generated if omitted.",
            example = "conv-abc-123")
    private String conversationId;

    @Schema(description = "Teams user ID", example = "user-001")
    private String userId;

    @Schema(description = "Display name of the user", example = "John Doe")
    private String userName;

    @Schema(description = "ISO-2 country code for country-specific policy lookup. AL or RS.",
            example = "AL", allowableValues = {"AL", "RS"})
    private String country;
}
