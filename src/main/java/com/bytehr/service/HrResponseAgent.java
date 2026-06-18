package com.bytehr.service;

import com.bytehr.api.dto.HrChatResponse;

import java.util.List;

public interface HrResponseAgent {

    /**
     * Generates an HR answer for the given question using retrieved document chunks.
     *
     * @param question        the user's question
     * @param country         ISO-2 country code for country-specific policy filtering
     * @param conversationId  Teams conversation ID for context
     * @param userId          Teams user ID
     * @param userName        display name of the user
     * @param conversationHistory previous exchanges in this conversation (formatted as alternating Q/A pairs)
     * @return chat response containing answer, citations, and confidence score
     */
    HrChatResponse answer(String question, String country, String conversationId,
                          String userId, String userName, List<String> conversationHistory);
}
