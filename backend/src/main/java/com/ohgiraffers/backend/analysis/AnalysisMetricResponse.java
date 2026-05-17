package com.ohgiraffers.backend.analysis;

public record AnalysisMetricResponse(
        String id,
        String title,
        int score,
        String status,
        String description
) {
}
