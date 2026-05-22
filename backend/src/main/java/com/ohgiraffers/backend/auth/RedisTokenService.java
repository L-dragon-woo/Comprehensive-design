package com.ohgiraffers.backend.auth;

import java.time.Duration;

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

    private String key(String username, String refreshToken) {
        return "auth:refresh:" + username + ":" + Integer.toHexString(refreshToken.hashCode());
    }
}
