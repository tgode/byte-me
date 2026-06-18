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
public class OllamaEmbeddingRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("prompt")
    private String prompt;
}
