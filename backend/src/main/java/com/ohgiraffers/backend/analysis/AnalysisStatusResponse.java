package com.ohgiraffers.backend.analysis;

import java.util.List;

public record AnalysisStatusResponse(
        String analysisId,
        AnalysisStatus status,
        int progress,
        String currentStep,
        List<AnalysisStepResponse> steps
) {
}
