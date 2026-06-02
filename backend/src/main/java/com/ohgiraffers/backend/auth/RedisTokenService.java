package com.ohgiraffers.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTokenService {
    private final StringRedisTemplate redis;

    public RedisTokenService(StringRedisTemplate redis) { this.redis = redis; }

    public void saveRefreshToken(String username, String refreshToken, long ttlSeconds) {
        redis.opsForValue().set(key(username, refreshToken), "active", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isRefreshTokenActive(String username, String refreshToken) {
        return Boolean.TRUE.equals(redis.hasKey(key(username, refreshToken)));
    }

    public void deleteRefreshToken(String username, String refreshToken) {
        redis.delete(key(username, refreshToken));
    }

    public void blacklistAccessToken(String accessToken, long ttlSeconds) {
        if (accessToken == null || accessToken.isBlank() || ttlSeconds <= 0) return;
        redis.opsForValue().set(accessBlacklistKey(accessToken), "blacklisted", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isAccessTokenBlacklisted(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return false;
        return Boolean.TRUE.equals(redis.hasKey(accessBlacklistKey(accessToken)));
    }

    private String key(String username, String refreshToken) {
        return "auth:refresh:" + username + ":" + Integer.toHexString(refreshToken.hashCode());
    }

    private String accessBlacklistKey(String accessToken) {
        return "auth:blacklist:access:" + sha256(accessToken);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
