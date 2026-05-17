package com.ohgiraffers.backend.analysis.presentation.dto;

import java.util.List;

public record AnalysisListResponse(
        List<AnalysisListItemResponse> items,
        int total
) {
}
