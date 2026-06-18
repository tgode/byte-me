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
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "teams_conversation_id", nullable = false, length = 500)
    private String teamsConversationId;

    @Column(name = "user_id", nullable = false, length = 500)
    private String userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "user_country", length = 10)
    private String userCountry;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    public void prePersist() {
        timestamp = Instant.now();
    }
}
