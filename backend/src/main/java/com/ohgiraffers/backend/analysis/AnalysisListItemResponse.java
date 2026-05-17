package com.ohgiraffers.backend.analysis;

import java.util.List;

public record AnalysisListItemResponse(
        String id,
        String date,
        String dateFormatted,
        int score,
        int change,
        List<String> improvements
) {
}
