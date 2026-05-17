package com.ohgiraffers.backend.analysis.presentation.dto;

import com.ohgiraffers.backend.analysis.domain.model.AnalysisStatus;

import java.time.Instant;

public record AnalysisCreateResponse(
        String analysisId,
        AnalysisStatus status,
        Instant createdAt
) {
}
