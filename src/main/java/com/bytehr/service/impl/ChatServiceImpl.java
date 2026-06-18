package com.bytehr.service.impl;

import com.bytehr.integration.ollama.OllamaClient;
import com.bytehr.integration.ollama.dto.OllamaMessage;
import com.bytehr.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final OllamaClient ollamaClient;

    @Override
    public String generateAnswer(List<OllamaMessage> messages) {
        return ollamaClient.generateAnswer(messages);
    }
}
