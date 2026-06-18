package com.bytehr.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound Bot Framework Activity response sent back to Microsoft Teams.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamsActivityResponse {

    @JsonProperty("type")
    @Builder.Default
    private String type = "message";

    @JsonProperty("text")
    private String text;

    @JsonProperty("textFormat")
    @Builder.Default
    private String textFormat = "markdown";
}
