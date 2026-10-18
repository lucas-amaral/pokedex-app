package com.example.tasks.config;

import com.example.tasks.entity.User;
import com.example.tasks.service.AccessToken;
import com.example.tasks.service.TokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

/** Adapter for {@link TokenIssuer}: HS256 JWTs whose subject is the user id. */
@Component
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(User user) {
        // JWT times are whole seconds; truncating keeps the expiry we report equal to the one in the token.
        var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var expiresAt = now.plus(properties.tokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().value().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("preferred_username", user.username().value())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
    }
}
