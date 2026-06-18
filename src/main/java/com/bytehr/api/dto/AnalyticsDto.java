package com.bytehr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDto {

    private UUID id;
    private UUID conversationId;
    private Long responseTimeMs;
    private BigDecimal confidenceScore;
    private String questionLanguage;
    private String userCountry;
    private Boolean wasAnswered;
    private Instant timestamp;
}
