package com.bytehr.integration.sharepoint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharePointFile {

    private String id;
    private String name;
    private String webUrl;
    private String downloadUrl;
    private Long size;
    private Instant lastModified;
    private String mimeType;
    private String fileExtension;
}
