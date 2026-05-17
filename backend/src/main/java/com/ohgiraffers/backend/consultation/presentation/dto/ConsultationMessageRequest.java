package com.ohgiraffers.backend.consultation.presentation.dto;

import java.util.List;

public record ConsultationMessageRequest(
        String analysisId,
        String message,
        List<ConsultationHistoryMessage> history
) {
}
