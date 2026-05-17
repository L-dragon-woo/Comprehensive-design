package com.ohgiraffers.backend.application.presentation.dto;

import java.util.List;

public record HospitalApplicationListResponse(
        List<HospitalApplicationListItemResponse> items,
        int total
) {
}
