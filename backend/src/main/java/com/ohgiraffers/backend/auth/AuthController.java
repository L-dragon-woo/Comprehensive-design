package com.ohgiraffers.backend.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.ohgiraffers.backend.auth.AuthDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
        UserDocument user = userService.authenticate(request.username(), request.password());
        return issueTokens(user.getUsername(), user.getDisplayName());
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registration data is required");
        }
        if (adminUsername.equalsIgnoreCase(request.username()) || userService.exists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }
        UserDocument user = userService.register(request.username(), request.password(), request.displayName());
        return issueTokens(user.getUsername(), user.getDisplayName());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        DecodedJWT jwt = jwtService.verify(request.refreshToken(), "refresh");
        String username = jwt.getSubject();
        if (!tokenService.isRefreshTokenActive(username, request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token is not active");
        }
        tokenService.deleteRefreshToken(username, request.refreshToken());
        return issueTokens(username);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody LogoutRequest request) {
        DecodedJWT jwt = jwtService.verify(request.refreshToken(), "refresh");
        tokenService.deleteRefreshToken(jwt.getSubject(), request.refreshToken());
    }

    private AuthResponse issueTokens(String username) {
        return issueTokens(username, username);
    }

    private AuthResponse issueTokens(String username, String displayName) {
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken(username);
        tokenService.saveRefreshToken(username, refreshToken, jwtService.refreshTtlSeconds());
        return new AuthResponse(accessToken, refreshToken, jwtService.accessTtlSeconds(), "Bearer", new UserProfile(username, displayName));
    }
}
