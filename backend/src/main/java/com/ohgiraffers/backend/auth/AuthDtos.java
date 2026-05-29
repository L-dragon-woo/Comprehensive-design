package com.ohgiraffers.backend.auth;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(
            String username,
            String password,
            String displayName,
            String gender,
            Integer age,
            String skinTreatmentHistory,
            Boolean hasAllergy,
            String allergyDetails,
            Boolean hasDisease,
            String diseaseDetails
    ) {}
    public record UpdateProfileRequest(
            String displayName,
            String gender,
            Integer age,
            String skinTreatmentHistory,
            Boolean hasAllergy,
            String allergyDetails,
            Boolean hasDisease,
            String diseaseDetails
    ) {}
    public record RefreshRequest(String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}
    public record AuthResponse(String accessToken, String refreshToken, long expiresIn, String tokenType, UserProfile user) {}
    public record UserProfile(
            String username,
            String displayName,
            String gender,
            Integer age,
            String skinTreatmentHistory,
            boolean hasAllergy,
            String allergyDetails,
            boolean hasDisease,
            String diseaseDetails
    ) {}
}
