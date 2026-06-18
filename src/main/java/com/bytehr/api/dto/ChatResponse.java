package com.bytehr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HR question answer with citations and confidence score")
public class ChatResponse {

    @Schema(description = "The generated HR answer", example = "You are entitled to 20 working days of annual vacation...")
    private String answer;

    @Schema(description = "Source documents used to generate the answer")
    private List<ChatCitation> citations;

    @Schema(description = "Semantic similarity confidence score (0.0–1.0). Scores below 0.6 trigger a low-confidence fallback.", example = "0.92")
    private double confidence;

    @Schema(description = "BCP-47 language code detected from the question", example = "en")
    private String detectedLanguage;

    @Schema(description = "Whether the question was answered from documents (false = low-confidence fallback used)")
    private boolean answered;
}
