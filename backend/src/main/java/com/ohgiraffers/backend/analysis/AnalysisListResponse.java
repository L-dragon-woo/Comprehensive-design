package com.ohgiraffers.backend.analysis;

import java.util.List;

public record AnalysisListResponse(
        List<AnalysisListItemResponse> items,
        int total
) {
}
