package com.example.tasks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * @param secret   HMAC key; HS256 needs at least 32 bytes
 * @param tokenTtl how long an access token is valid
 * @param issuer   written into tokens and required when reading them
 */
@ConfigurationProperties("tasks.security.jwt")
public record JwtProperties(String secret, Duration tokenTtl, String issuer) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        Objects.requireNonNull(secret, "secret is required");
        Objects.requireNonNull(tokenTtl, "tokenTtl is required");
        Objects.requireNonNull(issuer, "issuer is required");
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("tasks.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
    }
}
