package com.ohgiraffers.backend.consultation;

import java.util.List;

public record ConsultationMessageRequest(
        String analysisId,
        String message,
        List<ConsultationHistoryMessage> history
) {
}
