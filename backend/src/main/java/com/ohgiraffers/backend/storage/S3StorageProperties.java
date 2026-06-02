package com.ohgiraffers.backend.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String presignedUrlTtl
) {
    public S3StorageProperties {
        if (region == null || region.isBlank()) region = "ap-northeast-2";
        if (presignedUrlTtl == null || presignedUrlTtl.isBlank()) presignedUrlTtl = "PT1H";
    }

    public boolean enabled() {
        return bucket != null && !bucket.isBlank();
    }

    public Duration presignedUrlDuration() {
        return Duration.parse(presignedUrlTtl.trim());
    }
}
