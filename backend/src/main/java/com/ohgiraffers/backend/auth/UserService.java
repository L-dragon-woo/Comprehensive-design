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
        return register(username, password, displayName, null);
    }

    public UserEntity register(String username, String password, String displayName, AuthDtos.UpdateProfileRequest profile) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        String normalizedDisplayName = normalizeDisplayName(displayName, normalizedUsername);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        try {
            UserEntity user = new UserEntity(
                    normalizedUsername,
                    passwordEncoder.encode(password),
                    normalizedDisplayName,
                    Instant.now()
            );
            if (profile != null) {
                applyProfile(user, profile, normalizedUsername);
            }
            return userRepository.save(user);
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

    public UserEntity getByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    public UserEntity updateProfile(String username, AuthDtos.UpdateProfileRequest profile) {
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile data is required");
        }
        UserEntity user = getByUsername(username);
        applyProfile(user, profile, user.getUsername());
        return userRepository.save(user);
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

    private void applyProfile(UserEntity user, AuthDtos.UpdateProfileRequest profile, String username) {
        boolean hasAllergy = Boolean.TRUE.equals(profile.hasAllergy());
        boolean hasDisease = Boolean.TRUE.equals(profile.hasDisease());
        user.updateProfile(
                normalizeDisplayName(profile.displayName(), username),
                normalizeOptional(profile.gender(), 20, "gender must be 20 characters or less"),
                normalizeAge(profile.age()),
                normalizeOptional(profile.skinTreatmentHistory(), 5000, "skinTreatmentHistory must be 5000 characters or less"),
                hasAllergy,
                hasAllergy ? normalizeOptional(profile.allergyDetails(), 500, "allergyDetails must be 500 characters or less") : null,
                hasDisease,
                hasDisease ? normalizeOptional(profile.diseaseDetails(), 500, "diseaseDetails must be 500 characters or less") : null
        );
    }

    private Integer normalizeAge(Integer age) {
        if (age == null) {
            return null;
        }
        if (age < 0 || age > 130) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "age must be between 0 and 130");
        }
        return age;
    }

    private String normalizeOptional(String value, int maxLength, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return normalized;
    }
}
