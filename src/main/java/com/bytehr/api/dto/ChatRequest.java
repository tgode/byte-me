package com.bytehr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "question must not be blank")
    @Size(min = 1, max = 2000, message = "question must be between 1 and 2000 characters")
    @Schema(
        description = "The HR question to answer",
        example = "How many vacation days do employees receive?",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String question;

    @Pattern(regexp = "^(AL|RS)$", message = "country must be AL (Albania) or RS (Serbia)")
    @Schema(
        description = "ISO-2 country code for country-specific policy lookup. Optional.",
        example = "AL",
        allowableValues = {"AL", "RS"}
    )
    private String country;

    @Size(max = 128, message = "sessionId must not exceed 128 characters")
    @Schema(
        description = "Optional session ID for multi-turn conversations. " +
                      "Reuse the same ID across requests to maintain conversation context.",
        example = "session-abc-123"
    )
    private String sessionId;
}
