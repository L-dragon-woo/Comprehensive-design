package com.ohgiraffers.backend.analysis.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Analysis(
        String id,
        Instant createdAt,
        LocalDate date,
        int overallScore,
        String skinType,
        List<String> concerns,
        List<AnalysisMetricResponse> metrics,
        List<AnalysisTreatmentResponse> treatments,
        List<String> recommendations,
        String imageUrl,
        int change,
        List<String> improvements
) {
}
