package com.ohgiraffers.backend.storage;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3StorageService {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3StorageProperties properties;

    public S3StorageService(S3Client s3Client, S3Presigner presigner, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public StoredObject uploadAnalysisImage(String username, String analysisId, MultipartFile file) {
        String extension = extension(file.getOriginalFilename(), "jpg");
        String key = "images/%s/%s.%s".formatted(safeSegment(username), safeSegment(analysisId), extension);
        return upload(key, file, fallbackContentType(file.getContentType(), "image/jpeg"));
    }

    public StoredObject uploadReport(String username, String analysisId, MultipartFile file) {
        String extension = extension(file.getOriginalFilename(), "html");
        String suffix = analysisId == null || analysisId.isBlank() ? UUID.randomUUID().toString() : safeSegment(analysisId);
        String key = "reports/%s/%s.%s".formatted(safeSegment(username), suffix, extension);
        return upload(key, file, fallbackContentType(file.getContentType(), "text/html; charset=UTF-8"));
    }

    public StoredObject upload(String key, MultipartFile file, String contentType) {
        if (!enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 storage is not configured");
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredObject(key, presignedUrl(key), Instant.now().toString());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file upload failed", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 upload failed", e);
        }
    }

    public String presignedUrl(String key) {
        if (!enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 storage is not configured");
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();
        URL url = presigner.presignGetObject(presignRequest).url();
        return url.toString();
    }

    private String extension(String filename, String fallback) {
        if (filename == null) return fallback;
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) return fallback;
        String extension = filename.substring(index + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return extension.isBlank() ? fallback : extension;
    }

    private String safeSegment(String value) {
        return String.valueOf(value == null || value.isBlank() ? "anonymous" : value)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String fallbackContentType(String contentType, String fallback) {
        return contentType == null || contentType.isBlank() ? fallback : contentType;
    }

    public record StoredObject(String key, String url, String uploadedAt) {}
}
