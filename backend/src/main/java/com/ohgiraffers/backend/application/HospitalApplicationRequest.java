package com.ohgiraffers.backend.application;

import java.util.List;

public record HospitalApplicationRequest(
        String analysisId,
        String hospitalId,
        List<String> includedItems,
        boolean consent
) {
}
