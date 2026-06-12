package com.ohgiraffers.backend.config;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final ObjectMapper objectMapper;

    public ApiExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException e) {
        String message = e.getReason() == null || e.getReason().isBlank()
                ? e.getStatusCode().toString()
                : e.getReason();

        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of(
                        "status", e.getStatusCode().value(),
                        "message", message
                ));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "message", "database is unavailable"
                ));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequestException() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "message", "AI service is unavailable"
                ));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException e) {
        String responseBody = e.getResponseBodyAsString();
        String message = extractMessage(responseBody);
        HttpStatusCode status = switch (e.getStatusCode().value()) {
            case 429, 503 -> e.getStatusCode();
            default -> HttpStatus.BAD_GATEWAY;
        };

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "status", status.value(),
                        "message", message
                ));
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "AI service request failed";
        }
        try {
            JsonNode detail = objectMapper.readTree(responseBody).get("detail");
            if (detail != null && detail.isTextual()) {
                return detail.asText();
            }
        } catch (Exception ignored) {
            // Return the upstream body when it is not JSON.
        }
        return responseBody;
    }
}
