package com.example.tasks.config;

import com.example.tasks.entity.User;
import com.example.tasks.entity.UserId;
import com.example.tasks.entity.Username;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-0123456789";
    private static final User ALICE = new User(new UserId(UUID.randomUUID()), Username.of("alice"), "hash");

    @Test
    void issuesATokenTheDecoderAcceptsWithTheUserAsSubject() {
        var properties = new JwtProperties(SECRET, Duration.ofHours(1), "task-api");
        var issuer = new JwtTokenIssuer(SecurityConfig.createJwtEncoder(properties), properties, Clock.systemUTC());

        var token = issuer.issue(ALICE);
        var jwt = SecurityConfig.createJwtDecoder(properties).decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(ALICE.id().value().toString());
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("alice");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void expiresAfterTheConfiguredTime() {
        var now = Instant.parse("2026-10-18T10:00:00Z");
        var properties = new JwtProperties(SECRET, Duration.ofHours(1), "task-api");
        var issuer = new JwtTokenIssuer(SecurityConfig.createJwtEncoder(properties), properties, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(issuer.issue(ALICE).expiresAt()).isEqualTo(now.plus(Duration.ofHours(1)));
    }

    @Test
    void aTokenFromAnotherIssuerIsRefused() {
        var other = new JwtProperties(SECRET, Duration.ofHours(1), "someone-else");
        var token = new JwtTokenIssuer(SecurityConfig.createJwtEncoder(other), other, Clock.systemUTC()).issue(ALICE);
        var ours = SecurityConfig.createJwtDecoder(new JwtProperties(SECRET, Duration.ofHours(1), "task-api"));

        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> ours.decode(token.value()));
    }

    @Test
    void theDecoderRefusesASubjectThatIsNotAUserId() {
        var properties = new JwtProperties(SECRET, Duration.ofHours(1), "task-api");
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("task-api").subject("admin").issuedAt(now).expiresAt(now.plusSeconds(60)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = SecurityConfig.createJwtEncoder(properties).encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> SecurityConfig.createJwtDecoder(properties).decode(token));
    }

    @Test
    void refusesASecretTooShortForHs256() {
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties("short", Duration.ofHours(1), "task-api"));
    }
}
