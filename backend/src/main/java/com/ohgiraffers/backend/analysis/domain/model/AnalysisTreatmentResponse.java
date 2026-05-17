package com.ohgiraffers.backend.analysis.domain.model;

public record AnalysisTreatmentResponse(
        String id,
        String name,
        String match,
        String reason,
        String note
) {
}
