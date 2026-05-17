package com.ohgiraffers.backend.hospital;

import java.util.List;

public record HospitalListResponse(
        List<HospitalSummaryResponse> items,
        int total
) {
}
