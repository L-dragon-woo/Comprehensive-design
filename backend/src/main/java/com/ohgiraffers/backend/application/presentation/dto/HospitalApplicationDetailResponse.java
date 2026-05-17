package com.ohgiraffers.backend.application.presentation.dto;

import com.ohgiraffers.backend.application.domain.model.ApplicationStatus;

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
