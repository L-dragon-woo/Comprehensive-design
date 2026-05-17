package com.ohgiraffers.backend.application;

import java.util.List;

public record HospitalApplicationListResponse(
        List<HospitalApplicationListItemResponse> items,
        int total
) {
}
