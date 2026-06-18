package com.bytehr.integration.ollama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/embed (Ollama current API, replaces /api/embeddings).
 * The "input" field accepts a single string or a list; we use single-string mode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaEmbedRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("input")
    private String input;
}
