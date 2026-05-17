package com.ohgiraffers.backend.hospital.presentation.dto;

import java.util.List;

public record HospitalSummaryResponse(
        String id,
        String name,
        String distance,
        long distanceMeters,
        double rating,
        String address,
        List<String> specialties,
        List<String> matchedTreatments,
        String waitTime,
        String phone
) {
}
