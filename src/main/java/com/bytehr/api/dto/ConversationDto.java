package com.bytehr.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ConversationDto {

    private UUID id;
    private String teamsConversationId;
    private String userId;
    private String userName;
    private String userCountry;
    private String question;
    private String response;

    @JsonProperty("confidenceScore")
    private BigDecimal confidenceScore;

    private Instant timestamp;
}
