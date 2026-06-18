package com.bytehr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bytehr.rag")
@Data
public class RagProperties {

    /**
     * Maximum number of document chunks retrieved from pgvector per query.
     * Lower values reduce LLM context size and improve response latency.
     * Default: 3 (demo mode). Production: 5–10.
     */
    private int topK = 3;

    /**
     * Maximum total characters of document context sent to the LLM.
     * Chunks are included in relevance order; excess characters are truncated.
     * Default: 1500 (demo mode). Production: 3000–6000.
     */
    private int maxContextChars = 1500;

    /**
     * Enables strict RAG mode: the LLM is explicitly instructed to answer
     * only from the retrieved context and to say "I could not find this
     * information in the HR documents." when information is absent.
     * Also switches the temperature to 0.0 for deterministic output.
     * Default: true.
     */
    private boolean strictMode = true;
}
