package com.ohgiraffers.backend.application;

public record HospitalSummaryForApplicationResponse(
        String id,
        String name,
        String phone,
        String address
) {
}
