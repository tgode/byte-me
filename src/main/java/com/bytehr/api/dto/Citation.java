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
public class Citation {

    private UUID documentId;
    private String documentName;
    private String sourcePath;
    private String section;
    private Integer pageNumber;
    private String webUrl;
}
