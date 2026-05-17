package com.ohgiraffers.backend.analysis.presentation.dto;

import com.ohgiraffers.backend.analysis.domain.model.AnalysisStatus;

import java.util.List;

public record AnalysisStatusResponse(
        String analysisId,
        AnalysisStatus status,
        int progress,
        String currentStep,
        List<AnalysisStepResponse> steps
) {
}
