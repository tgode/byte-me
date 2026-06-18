package com.bytehr.service;

import com.bytehr.integration.ollama.dto.OllamaMessage;

import java.util.List;

public interface ChatService {

    /**
     * Generates an answer from the LLM given a list of conversation messages.
     */
    String generateAnswer(List<OllamaMessage> messages);
}
