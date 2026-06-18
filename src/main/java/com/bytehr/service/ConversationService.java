package com.bytehr.service;

import com.bytehr.model.Conversation;

import java.util.List;

public interface ConversationService {

    /**
     * Retrieves recent conversation history (question/response pairs) for a Teams conversation.
     */
    List<String> getConversationHistory(String teamsConversationId, int maxTurns);

    /**
     * Persists a conversation turn to the database.
     */
    Conversation saveConversation(String teamsConversationId, String userId, String userName,
                                  String country, String question, String response, double confidenceScore);
}
