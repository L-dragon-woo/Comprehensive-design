package com.ohgiraffers.backend.application.presentation.dto;

import java.util.List;

public record HospitalApplicationRequest(
        String analysisId,
        String hospitalId,
        List<String> includedItems,
        boolean consent
) {
}
