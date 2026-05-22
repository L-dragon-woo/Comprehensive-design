package com.ohgiraffers.backend.auth;

import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String createAccessToken(String username) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().plusSeconds(accessTtlSeconds))
                .sign(algorithm);
    }

    public String createRefreshToken(String username) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withClaim("type", "refresh")
                .withExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token, String type) {
        DecodedJWT jwt = verifier.verify(token);
        if (!type.equals(jwt.getClaim("type").asString())) throw new IllegalArgumentException("Invalid token type");
        return jwt;
    }

    public long accessTtlSeconds() { return accessTtlSeconds; }
    public long refreshTtlSeconds() { return refreshTtlSeconds; }
}
