package com.ohgiraffers.backend.storage;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class StorageController {
    private final S3StorageService storageService;

    public StorageController(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/api/files/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3StorageService.StoredObject uploadReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String analysisId,
            Authentication authentication
    ) {
        return storageService.uploadReport(authentication.getName(), analysisId, file);
    }

    @GetMapping("/api/files/url")
    public S3StorageService.StoredObject getFileUrl(
            @RequestParam String key,
            Authentication authentication
    ) {
        String username = authentication.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!key.startsWith("reports/" + username + "/") && !key.startsWith("images/" + username + "/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "file key is not owned by current user");
        }
        return new S3StorageService.StoredObject(key, storageService.presignedUrl(key), null);
    }
}
