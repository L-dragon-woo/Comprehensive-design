package com.ohgiraffers.backend.auth;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class UserDocument {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String passwordHash;
    private String displayName;
    private Instant createdAt;

    public UserDocument() {
    }

    public UserDocument(String username, String passwordHash, String displayName, Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
