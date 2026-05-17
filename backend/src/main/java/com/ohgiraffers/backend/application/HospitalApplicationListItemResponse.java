package com.ohgiraffers.backend.application;

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
