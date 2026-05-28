package com.ohgiraffers.backend.auth;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserEntity register(String username, String password, String displayName) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        String normalizedDisplayName = normalizeDisplayName(displayName, normalizedUsername);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        try {
            return userRepository.save(new UserEntity(
                    normalizedUsername,
                    passwordEncoder.encode(password),
                    normalizedDisplayName,
                    Instant.now()
            ));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists", e);
        }
    }

    public UserEntity authenticate(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserEntity user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (!passwordEncoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        return user;
    }

    public boolean exists(String username) {
        return userRepository.existsByUsername(normalizeUsername(username));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || normalized.length() > 254) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must be a valid email address");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 4 || password.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be 4-100 characters");
        }
    }

    private String normalizeDisplayName(String displayName, String username) {
        if (displayName == null || displayName.isBlank()) {
            return username;
        }
        String normalized = displayName.trim();
        if (normalized.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName must be 40 characters or less");
        }
        return normalized;
    }
}
