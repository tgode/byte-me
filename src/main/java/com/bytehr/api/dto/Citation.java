package com.bytehr.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * Internal filesystem or SharePoint path — NEVER serialized to API responses.
     * Kept internally for logging and analytics only.
     */
    @JsonIgnore
    private String sourcePath;

    private String section;
    private Integer pageNumber;

    /**
     * SharePoint web URL shown to users as a clickable link.
     * Null for local-mode documents — no link is shown.
     */
    private String webUrl;
}
