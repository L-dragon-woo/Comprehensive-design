package com.ohgiraffers.backend.hospital.domain.model;

import java.util.List;

public record Hospital(
        String id,
        String name,
        double rating,
        String address,
        String phone,
        double latitude,
        double longitude,
        List<String> specialties,
        List<TreatmentInfo> treatments,
        List<String> availableTimes
) {
}
