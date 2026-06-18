package com.bytehr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {

    private long totalQuestions;
    private long answeredQuestions;
    private long unansweredQuestions;
    private double answerRate;
    private Double avgResponseTimeMs;
    private Double avgConfidenceScore;
    private List<AnalyticsDto> recentEntries;
}
