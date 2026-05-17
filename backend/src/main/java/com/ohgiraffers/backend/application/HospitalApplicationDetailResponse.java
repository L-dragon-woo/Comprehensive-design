package com.ohgiraffers.backend.application;

import java.time.Instant;
import java.util.List;

public record HospitalApplicationDetailResponse(
        String id,
        String analysisId,
        HospitalSummaryForApplicationResponse hospital,
        Instant submittedAt,
        ApplicationStatus status,
        List<String> includedItems
) {
}
