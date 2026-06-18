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
public class OllamaChatOptions {

    @JsonProperty("temperature")
    @Builder.Default
    private double temperature = 0.1;

    @JsonProperty("num_ctx")
    @Builder.Default
    private int numCtx = 4096;

    /**
     * Disables extended chain-of-thought (thinking) mode on qwen3 and similar models.
     * Setting to false significantly reduces latency by suppressing reasoning tokens.
     */
    @JsonProperty("think")
    @Builder.Default
    private boolean think = false;
}
