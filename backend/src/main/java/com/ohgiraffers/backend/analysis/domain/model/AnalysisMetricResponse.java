package com.ohgiraffers.backend.analysis.domain.model;

public record AnalysisMetricResponse(
        String id,
        String title,
        int score,
        String status,
        String description
) {
}
