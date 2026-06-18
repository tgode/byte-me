package com.bytehr.api;

import com.bytehr.api.dto.AnalyticsDto;
import com.bytehr.api.dto.AnalyticsSummaryDto;
import com.bytehr.model.Analytics;
import com.bytehr.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.OptionalDouble;

/**
 * REST endpoint for analytics data.
 *
 * GET /api/analytics — returns a usage and quality summary.
 */
@RestController
@RequestMapping("/api/analytics")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsRepository analyticsRepository;

    @GetMapping
    public ResponseEntity<AnalyticsSummaryDto> getAnalytics(
            @RequestParam(defaultValue = "50") int limit) {

        List<Analytics> all = analyticsRepository.findAll();
        List<Analytics> recent = analyticsRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent();

        long total      = all.size();
        long answered   = all.stream().filter(a -> Boolean.TRUE.equals(a.getWasAnswered())).count();
        long unanswered = total - answered;

        double answerRate = total > 0 ? (double) answered / total * 100 : 0.0;

        OptionalDouble avgResponseTime = all.stream()
                .filter(a -> a.getResponseTimeMs() != null)
                .mapToLong(Analytics::getResponseTimeMs)
                .average();

        OptionalDouble avgConfidence = all.stream()
                .filter(a -> a.getConfidenceScore() != null)
                .mapToDouble(a -> a.getConfidenceScore().doubleValue())
                .average();

        List<AnalyticsDto> recentDtos = recent.stream().map(this::toDto).toList();

        return ResponseEntity.ok(AnalyticsSummaryDto.builder()
                .totalQuestions(total)
                .answeredQuestions(answered)
                .unansweredQuestions(unanswered)
                .answerRate(Math.round(answerRate * 100.0) / 100.0)
                .avgResponseTimeMs(avgResponseTime.isPresent() ? avgResponseTime.getAsDouble() : null)
                .avgConfidenceScore(avgConfidence.isPresent() ? avgConfidence.getAsDouble() : null)
                .recentEntries(recentDtos)
                .build());
    }

    private AnalyticsDto toDto(Analytics a) {
        return AnalyticsDto.builder()
                .id(a.getId())
                .conversationId(a.getConversationId())
                .responseTimeMs(a.getResponseTimeMs())
                .confidenceScore(a.getConfidenceScore())
                .questionLanguage(a.getQuestionLanguage())
                .userCountry(a.getUserCountry())
                .wasAnswered(a.getWasAnswered())
                .timestamp(a.getTimestamp())
                .build();
    }
}
