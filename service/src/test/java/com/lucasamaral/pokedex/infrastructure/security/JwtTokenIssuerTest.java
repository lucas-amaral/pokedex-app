package com.lucasamaral.pokedex.infrastructure.security;

import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.Role;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwtTokenIssuerTest {

    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private static final JwtProperties PROPERTIES =
            new JwtProperties("a-test-secret-that-is-at-least-32-bytes-long", Duration.ofHours(2), "pokedex-service");

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(
            SecurityConfig.createJwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
    private final User ash = new User(UUID.randomUUID(), Username.of("ash"), Email.of("ash@pallet.town"), "hash", Role.USER, NOW);

    @Test
    void issuesASignedTokenNamingTheUserAndTheirRole() {
        var token = issuer.issue(ash);

        var jwt = SecurityConfig.createJwtDecoder(PROPERTIES).decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo(ash.id().toString());
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("ash");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("pokedex-service");
    }

    @Test
    void expiresAfterTheConfiguredTime() {
        var token = issuer.issue(ash);

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
        assertThat(SecurityConfig.createJwtDecoder(PROPERTIES).decode(token.value()).getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        var otherSecret = new JwtProperties("another-secret-that-is-also-32-bytes-long", Duration.ofHours(2), "pokedex-service");
        var forged = new JwtTokenIssuer(SecurityConfig.createJwtEncoder(otherSecret), otherSecret, Clock.systemUTC()).issue(ash);

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> SecurityConfig.createJwtDecoder(PROPERTIES).decode(forged.value()));
    }

    @Test
    void refusesASecretThatIsTooShortForHs256() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(2), "pokedex-service"));
    }
}
