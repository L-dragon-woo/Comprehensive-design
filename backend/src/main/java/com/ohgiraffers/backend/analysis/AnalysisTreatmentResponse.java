package com.ohgiraffers.backend.analysis;

public record AnalysisTreatmentResponse(
        String id,
        String name,
        String match,
        String reason,
        String note
) {
}
