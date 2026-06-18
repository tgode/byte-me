package com.bytehr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bytehr.security")
@Data
public class SecurityProperties {

    private Redaction redaction = new Redaction();

    @Data
    public static class Redaction {

        /**
         * When true, PII patterns (email, phone, IBAN, national IDs) are replaced
         * with redaction tokens before text is chunked and embedded.
         * This prevents sensitive data from entering the vector database.
         */
        private boolean enabled = true;
    }
}
