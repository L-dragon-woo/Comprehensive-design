package com.ohgiraffers.backend.application;

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
