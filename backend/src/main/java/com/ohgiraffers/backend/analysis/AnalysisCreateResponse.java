package com.ohgiraffers.backend.analysis;

import java.time.Instant;

public record AnalysisCreateResponse(
        String analysisId,
        AnalysisStatus status,
        Instant createdAt
) {
}
