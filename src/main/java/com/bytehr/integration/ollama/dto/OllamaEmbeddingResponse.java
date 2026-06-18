package com.bytehr.integration.ollama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaEmbeddingResponse {

    @JsonProperty("embedding")
    private List<Float> embedding;
}
