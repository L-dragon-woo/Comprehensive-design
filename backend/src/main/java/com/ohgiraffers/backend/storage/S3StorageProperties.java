package com.ohgiraffers.backend.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        Duration presignedUrlTtl
) {
    public S3StorageProperties {
        if (region == null || region.isBlank()) region = "ap-northeast-2";
        if (presignedUrlTtl == null) presignedUrlTtl = Duration.ofHours(1);
    }

    public boolean enabled() {
        return bucket != null && !bucket.isBlank();
    }
}
