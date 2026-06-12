package com.ohgiraffers.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tools.jackson.databind.ObjectMapper;

class ApiExceptionHandlerTests {
    private final ApiExceptionHandler handler = new ApiExceptionHandler(new ObjectMapper());

    @Test
    void preservesQueueFullStatusAndExtractsFastApiDetail() {
        ResponseEntity<Map<String, Object>> response = handler.handleWebClientResponseException(
                upstreamError(HttpStatus.TOO_MANY_REQUESTS, "{\"detail\":\"queue full\"}")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("status", 429).containsEntry("message", "queue full");
    }

    @Test
    void preservesQueueTimeoutStatusAndExtractsFastApiDetail() {
        ResponseEntity<Map<String, Object>> response = handler.handleWebClientResponseException(
                upstreamError(HttpStatus.SERVICE_UNAVAILABLE, "{\"detail\":\"queue timed out\"}")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", 503).containsEntry("message", "queue timed out");
    }

    private WebClientResponseException upstreamError(HttpStatus status, String body) {
        return WebClientResponseException.create(
                status.value(),
                status.getReasonPhrase(),
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
    }
}
