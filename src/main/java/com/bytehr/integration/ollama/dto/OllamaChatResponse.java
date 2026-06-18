package com.bytehr.integration.ollama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaChatResponse {

    @JsonProperty("model")
    private String model;

    @JsonProperty("message")
    private OllamaMessage message;

    @JsonProperty("done")
    private boolean done;

    @JsonProperty("total_duration")
    private Long totalDuration;
}
