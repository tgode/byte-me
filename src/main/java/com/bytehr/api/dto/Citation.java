package com.bytehr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {

    private String documentName;
    private String sourcePath;
    private String section;
    private Integer pageNumber;
    private String webUrl;
}
