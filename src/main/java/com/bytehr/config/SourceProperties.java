package com.bytehr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bytehr.source")
@Data
public class SourceProperties {

    /** Document source type: "local" or "sharepoint". */
    private String type = "local";

    /** Filesystem path scanned when type=local. May be relative or absolute. */
    private String localPath = "./sample-data/hr-documents";
}
