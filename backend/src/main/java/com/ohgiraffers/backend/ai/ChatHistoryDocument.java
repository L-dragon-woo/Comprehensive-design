package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("chat_history")
public class ChatHistoryDocument {
    @Id
    private String id;
    private String username;
    private String sessionId;
    private String userMessage;
    private Map<String, Object> aiResponse;
    private Object analysis;
    private String analysisId;
    private Object requestHistory;
    private Instant createdAt;

    public ChatHistoryDocument() {
    }

    public ChatHistoryDocument(
            String username,
            String sessionId,
            String userMessage,
            Map<String, Object> aiResponse,
            Object analysis,
            String analysisId,
            Object requestHistory,
            Instant createdAt
    ) {
        this.username = username;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.analysis = analysis;
        this.analysisId = analysisId;
        this.requestHistory = requestHistory;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Map<String, Object> getAiResponse() {
        return aiResponse;
    }

    public Object getAnalysis() {
        return analysis;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public Object getRequestHistory() {
        return requestHistory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
