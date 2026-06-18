package com.bytehr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelevantChunk {

    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private String sourcePath;
    private String content;
    private double similarityScore;
    private Integer chunkIndex;
    private Integer pageNumber;
}
