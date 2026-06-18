package com.bytehr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "question_language", length = 10)
    private String questionLanguage;

    @Column(name = "user_country", length = 10)
    private String userCountry;

    @Column(name = "was_answered")
    private Boolean wasAnswered;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    public void prePersist() {
        timestamp = Instant.now();
    }
}
