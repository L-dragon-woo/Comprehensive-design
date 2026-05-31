package com.ohgiraffers.backend.ai;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("analysis_results")
public class AnalysisResultDocument {
    @Id
    private String id;
    private String username;
    private Map<String, Object> analysis;
    private Instant createdAt;

    public AnalysisResultDocument() {
    }

    public AnalysisResultDocument(String username, Map<String, Object> analysis, Instant createdAt) {
        this.username = username;
        this.analysis = analysis;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Map<String, Object> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Map<String, Object> analysis) {
        this.analysis = analysis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
