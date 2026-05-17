package com.ohgiraffers.backend.hospital.presentation.dto;

import java.util.List;

public record HospitalListResponse(
        List<HospitalSummaryResponse> items,
        int total
) {
}
