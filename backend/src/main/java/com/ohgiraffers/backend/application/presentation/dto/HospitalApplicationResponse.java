package com.ohgiraffers.backend.application.presentation.dto;

import com.ohgiraffers.backend.application.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.List;

public record HospitalApplicationResponse(
        String id,
        String analysisId,
        String hospitalId,
        String hospitalName,
        Instant submittedAt,
        ApplicationStatus status,
        List<String> includedItems
) {
}
