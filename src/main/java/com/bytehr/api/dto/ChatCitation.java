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
@Schema(description = "Source document citation included in an HR answer")
public class ChatCitation {

    @Schema(description = "Name of the source document", example = "vacation-policy.md")
    private String document;

    @Schema(description = "Section or heading within the document", example = "Annual Vacation Days")
    private String section;

    @Schema(description = "File path or SharePoint URL of the source document")
    private String sourcePath;

    @Schema(description = "Page number within the document, if available", example = "3")
    private Integer pageNumber;
}
