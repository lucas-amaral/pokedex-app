package com.lucasamaral.pokedex.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Settings for the access tokens this service issues and accepts.
 *
 * @param secret   HMAC key shared by issuing and checking; at least 32 bytes, as HS256 requires
 * @param tokenTtl how long a token stays valid
 * @param issuer   the "iss" claim written and required
 */
@ConfigurationProperties("pokedex.security.jwt")
public record JwtProperties(String secret, Duration tokenTtl, String issuer) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        Objects.requireNonNull(secret, "pokedex.security.jwt.secret is required");
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("pokedex.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        Objects.requireNonNull(tokenTtl, "pokedex.security.jwt.token-ttl is required");
        Objects.requireNonNull(issuer, "pokedex.security.jwt.issuer is required");
    }
}
