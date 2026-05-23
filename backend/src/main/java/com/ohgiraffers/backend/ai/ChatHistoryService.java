package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ChatHistoryService {
    private final ChatHistoryRepository repository;

    public ChatHistoryService(ChatHistoryRepository repository) {
        this.repository = repository;
    }

    public ChatHistoryDocument saveTurn(String username, ChatMessageRequest request, Map<String, Object> aiResponse) {
        String responseSessionId = aiResponse == null ? null : stringValue(aiResponse.get("sessionId"));
        String sessionId = responseSessionId == null ? request.sessionId() : responseSessionId;
        ChatHistoryDocument history = new ChatHistoryDocument(
                username,
                sessionId,
                request.message(),
                aiResponse,
                request.analysis(),
                request.analysisId(),
                request.history(),
                Instant.now()
        );
        return repository.save(history);
    }

    private String stringValue(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
