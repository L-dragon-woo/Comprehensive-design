package com.ohgiraffers.backend.common;

import java.time.Instant;

public record ApiErrorResponse(
        String message,
        Instant timestamp
) {
}
