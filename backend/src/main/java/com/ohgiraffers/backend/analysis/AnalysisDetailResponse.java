package com.ohgiraffers.backend.analysis;

import java.util.List;

public record AnalysisDetailResponse(
        String id,
        String date,
        String dateFormatted,
        int overallScore,
        String skinType,
        List<String> concerns,
        List<AnalysisMetricResponse> metrics,
        List<AnalysisTreatmentResponse> treatments,
        List<String> recommendations,
        String imageUrl
) {
}
