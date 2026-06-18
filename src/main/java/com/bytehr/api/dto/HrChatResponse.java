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
public class HrChatResponse {

    private String answer;
    private List<Citation> citations;
    private double confidenceScore;
    private String detectedLanguage;
    private boolean answered;
}
