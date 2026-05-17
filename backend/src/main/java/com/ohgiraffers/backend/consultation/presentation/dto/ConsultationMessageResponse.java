package com.ohgiraffers.backend.consultation.presentation.dto;

import java.time.Instant;

public record ConsultationMessageResponse(
        String messageId,
        String role,
        String content,
        Instant createdAt
) {
}
