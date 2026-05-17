package com.ohgiraffers.backend.application.presentation.dto;

public record HospitalSummaryForApplicationResponse(
        String id,
        String name,
        String phone,
        String address
) {
}
