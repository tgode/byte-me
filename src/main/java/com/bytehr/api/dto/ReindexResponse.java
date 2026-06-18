package com.bytehr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a full document reindex operation")
public class ReindexResponse {

    private String status;

    @Schema(description = "Number of documents discovered and processed")
    private int documentsProcessed;

    @Schema(description = "Total chunk rows created in document_chunks table")
    private long chunksCreated;

    @Schema(description = "Total chunks that received a vector embedding")
    private long embeddingsCreated;

    @Schema(description = "Wall-clock duration of the reindex in milliseconds")
    private long durationMs;

    private String message;
}
