package com.ohgiraffers.backend.hospital;

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
