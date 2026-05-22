package com.ohgiraffers.backend.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinai.ai")
public record AiClientProperties(String baseUrl) {
    public AiClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000";
        }
    }
}
