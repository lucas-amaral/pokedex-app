package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.InvalidFieldException;
import com.lucasamaral.pokedex.domain.user.Role;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.support.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final UserAccountService accounts = new UserAccountService(
            users, new PrefixPasswordHasher(), new FakeTokenIssuer(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final RegisterUserUseCase register = accounts;
    private final AuthenticateUserUseCase authenticate = accounts;
    private final GetCurrentUserUseCase currentUser = accounts;

    @Test
    void registersARegularUserWithAHashedPassword() {
        var ash = register.register(new Registration(" Ash ", "Ash@Pallet.Town", "pikachu123"));

        assertThat(ash.username().value()).isEqualTo("ash");
        assertThat(ash.email().value()).isEqualTo("ash@pallet.town");
        assertThat(ash.role()).isEqualTo(Role.USER);
        assertThat(ash.passwordHash()).isEqualTo("hashed:pikachu123");
        assertThat(ash.createdAt()).isEqualTo(NOW);
        assertThat(users.all()).containsExactly(ash);
    }

    @Test
    void refusesAUsernameThatIsTaken() {
        register.register(new Registration("ash", "ash@pallet.town", "pikachu123"));

        assertThatExceptionOfType(UsernameTakenException.class)
                .isThrownBy(() -> register.register(new Registration("ASH", "other@pallet.town", "pikachu123")));
    }

    @Test
    void refusesAnEmailThatIsTaken() {
        register.register(new Registration("ash", "ash@pallet.town", "pikachu123"));

        assertThatExceptionOfType(EmailTakenException.class)
                .isThrownBy(() -> register.register(new Registration("misty", "ASH@pallet.town", "togepi123")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "        ", "a-password-that-is-much-longer-than-seventy-two-characters-which-bcrypt-would-cut"})
    void refusesAPasswordOutsideTheAllowedLength(String password) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> register.register(new Registration("ash", "ash@pallet.town", password)))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("password"));
        assertThat(users.all()).isEmpty();
    }

    @Test
    void logsInWithTheRightPasswordAndGetsAToken() {
        var ash = register.register(new Registration("ash", "ash@pallet.town", "pikachu123"));

        var session = authenticate.authenticate(" ASH ", "pikachu123");

        assertThat(session.user()).isEqualTo(ash);
        assertThat(session.token().value()).isEqualTo("token-for-" + ash.id());
        assertThat(session.token().expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
    }

    @Test
    void refusesAWrongPasswordAndAnUnknownUserTheSameWay() {
        register.register(new Registration("ash", "ash@pallet.town", "pikachu123"));

        var wrongPassword = catchInvalidCredentials("ash", "charmander1");
        var unknownUser = catchInvalidCredentials("gary", "eevee12345");

        assertThat(wrongPassword.getMessage()).isEqualTo(unknownUser.getMessage());
    }

    @Test
    void refusesAMalformedUsernameAsInvalidCredentials() {
        catchInvalidCredentials("not valid!", "pikachu123");
    }

    @Test
    void findsTheCurrentUser() {
        var ash = register.register(new Registration("ash", "ash@pallet.town", "pikachu123"));

        assertThat(currentUser.currentUser(ash.id())).isEqualTo(ash);
    }

    @Test
    void reportsACurrentUserThatNoLongerExists() {
        assertThatExceptionOfType(InvalidCredentialsException.class)
                .isThrownBy(() -> currentUser.currentUser(UUID.randomUUID()));
    }

    private InvalidCredentialsException catchInvalidCredentials(String username, String password) {
        try {
            authenticate.authenticate(username, password);
        } catch (InvalidCredentialsException expected) {
            return expected;
        }
        throw new AssertionError("Expected InvalidCredentialsException for " + username);
    }

    /** "Hashes" by prefixing, so tests can see the hasher was used without real BCrypt. */
    private static final class PrefixPasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals("hashed:" + rawPassword);
        }
    }

    private static final class FakeTokenIssuer implements TokenIssuer {

        @Override
        public AccessToken issue(User user) {
            return new AccessToken("token-for-" + user.id(), NOW.plus(Duration.ofHours(2)));
        }
    }
}
