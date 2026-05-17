package com.ohgiraffers.backend.application.presentation.dto;

import com.ohgiraffers.backend.application.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.List;

public record HospitalApplicationListItemResponse(
        String id,
        String hospitalName,
        Instant submittedAt,
        ApplicationStatus status,
        List<String> includedItems
) {
}
