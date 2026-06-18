package com.bytehr.integration.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response body from POST /api/embed.
 * "embeddings" is an array-of-arrays: one inner list per input string.
 * For single-string input, embeddings.get(0) is the embedding vector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaEmbedResponse {

    @JsonProperty("model")
    private String model;

    @JsonProperty("embeddings")
    private List<List<Float>> embeddings;

    @JsonProperty("total_duration")
    private Long totalDuration;

    @JsonProperty("load_duration")
    private Long loadDuration;

    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;
}
