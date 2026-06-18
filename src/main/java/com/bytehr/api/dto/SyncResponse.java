package com.bytehr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {

    private String status;
    private int documentsProcessed;
    private long totalDocumentsIndexed;
    private long durationMs;
    private String sourceType;
    private String message;
}
