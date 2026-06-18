package com.bytehr.service.impl;

import com.bytehr.model.Conversation;
import com.bytehr.repository.ConversationRepository;
import com.bytehr.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;

    @Override
    public List<String> getConversationHistory(String teamsConversationId, int maxTurns) {
        List<Conversation> turns = conversationRepository
                .findByTeamsConversationIdOrderByTimestampAsc(teamsConversationId);

        List<String> history = new ArrayList<>();
        int start = Math.max(0, turns.size() - maxTurns);
        for (int i = start; i < turns.size(); i++) {
            Conversation turn = turns.get(i);
            history.add(turn.getQuestion());
            if (turn.getResponse() != null) {
                history.add(turn.getResponse());
            }
        }
        return history;
    }

    @Override
    public Conversation saveConversation(String teamsConversationId, String userId, String userName,
                                         String country, String question, String response,
                                         double confidenceScore) {
        Conversation conversation = Conversation.builder()
                .teamsConversationId(teamsConversationId)
                .userId(userId)
                .userName(userName)
                .userCountry(country)
                .question(question)
                .response(response)
                .confidenceScore(BigDecimal.valueOf(confidenceScore))
                .build();
        return conversationRepository.save(conversation);
    }
}
