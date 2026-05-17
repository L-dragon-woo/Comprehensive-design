package com.ohgiraffers.backend.application.domain.model;

import java.time.Instant;
import java.util.List;

public record HospitalApplication(
        String id,
        String analysisId,
        String hospitalId,
        String hospitalName,
        Instant submittedAt,
        ApplicationStatus status,
        List<String> includedItems
) {
}
