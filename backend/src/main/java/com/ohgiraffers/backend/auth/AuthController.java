package com.ohgiraffers.backend.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.ohgiraffers.backend.auth.AuthDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final String adminUsername;
    private final String adminPassword;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenService tokenService;
    private final UserService userService;

    public AuthController(
            @Value("${app.auth.admin-username}") String adminUsername,
            @Value("${app.auth.admin-password}") String adminPassword,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RedisTokenService tokenService,
            UserService userService
    ) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        if (adminUsername.equals(request.username())) {
            boolean passwordMatches = adminPassword.startsWith("{bcrypt}")
                    ? passwordEncoder.matches(request.password(), adminPassword.substring("{bcrypt}".length()))
                    : request.password().equals(adminPassword);
            if (!passwordMatches) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
            }
            return issueTokens(request.username(), "SkinAI Admin");
        }
        UserEntity user = userService.authenticate(request.username(), request.password());
        return issueTokens(user);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registration data is required");
        }
        if (adminUsername.equalsIgnoreCase(request.username()) || userService.exists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }
        UserEntity user = userService.register(
                request.username(),
                request.password(),
                request.displayName(),
                new UpdateProfileRequest(
                        request.displayName(),
                        request.gender(),
                        request.age(),
                        request.skinTreatmentHistory(),
                        request.hasAllergy(),
                        request.allergyDetails(),
                        request.hasDisease(),
                        request.diseaseDetails()
                )
        );
        return issueTokens(user);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        DecodedJWT jwt = jwtService.verify(request.refreshToken(), "refresh");
        String username = jwt.getSubject();
        if (!tokenService.isRefreshTokenActive(username, request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token is not active");
        }
        tokenService.deleteRefreshToken(username, request.refreshToken());
        if (adminUsername.equals(username)) {
            return issueTokens(username, "SkinAI Admin");
        }
        return issueTokens(userService.getByUsername(username));
    }

    @GetMapping("/me")
    public UserProfile me(Authentication authentication) {
        if (adminUsername.equals(authentication.getName())) {
            return adminProfile();
        }
        return toProfile(userService.getByUsername(authentication.getName()));
    }

    @PutMapping("/me")
    public UserProfile updateMe(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        if (adminUsername.equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin profile cannot be updated");
        }
        return toProfile(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody LogoutRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
        DecodedJWT jwt = jwtService.verify(request.refreshToken(), "refresh");
        tokenService.deleteRefreshToken(jwt.getSubject(), request.refreshToken());
        String accessToken = bearerToken(authorization);
        if (accessToken != null) {
            try {
                DecodedJWT accessJwt = jwtService.verify(accessToken, "access");
                tokenService.blacklistAccessToken(accessToken, jwtService.remainingTtlSeconds(accessJwt));
            } catch (RuntimeException ignored) {
                // Expired or malformed access tokens do not need blacklist storage.
            }
        }
    }

    private AuthResponse issueTokens(String username) {
        return issueTokens(username, username);
    }

    private AuthResponse issueTokens(String username, String displayName) {
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken(username);
        tokenService.saveRefreshToken(username, refreshToken, jwtService.refreshTtlSeconds());
        return new AuthResponse(accessToken, refreshToken, jwtService.accessTtlSeconds(), "Bearer", new UserProfile(username, displayName, null, null, null, false, null, false, null));
    }

    private AuthResponse issueTokens(UserEntity user) {
        String accessToken = jwtService.createAccessToken(user.getUsername());
        String refreshToken = jwtService.createRefreshToken(user.getUsername());
        tokenService.saveRefreshToken(user.getUsername(), refreshToken, jwtService.refreshTtlSeconds());
        return new AuthResponse(accessToken, refreshToken, jwtService.accessTtlSeconds(), "Bearer", toProfile(user));
    }

    private UserProfile toProfile(UserEntity user) {
        return new UserProfile(
                user.getUsername(),
                user.getDisplayName(),
                user.getGender(),
                user.getAge(),
                user.getSkinTreatmentHistory(),
                user.isHasAllergy(),
                user.getAllergyDetails(),
                user.isHasDisease(),
                user.getDiseaseDetails()
        );
    }

    private UserProfile adminProfile() {
        return new UserProfile(adminUsername, "SkinAI Admin", null, null, null, false, null, false, null);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
