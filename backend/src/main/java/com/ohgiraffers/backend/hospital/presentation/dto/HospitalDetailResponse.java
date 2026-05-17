package com.ohgiraffers.backend.hospital.presentation.dto;

import com.ohgiraffers.backend.hospital.domain.model.TreatmentInfo;

import java.util.List;

public record HospitalDetailResponse(
        String id,
        String name,
        double rating,
        String address,
        String phone,
        List<String> specialties,
        List<TreatmentInfo> treatments,
        List<String> availableTimes
) {
}
