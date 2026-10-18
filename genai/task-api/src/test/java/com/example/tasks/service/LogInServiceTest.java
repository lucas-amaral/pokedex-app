package com.example.tasks.service;

import com.example.tasks.entity.InvalidFieldException;
import com.example.tasks.entity.User;
import com.example.tasks.entity.UserId;
import com.example.tasks.entity.Username;
import com.example.tasks.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LogInServiceTest {

    private static final User ALICE = new User(new UserId(UUID.randomUUID()), Username.of("alice"), "hash:wonderland123");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final CountingPasswordHasher hasher = new CountingPasswordHasher();
    private final LogInUseCase logIn = new LogInService(users, hasher, FakeTokenIssuer.INSTANCE);

    @Test
    void issuesATokenForTheRightPassword() {
        users.add(ALICE);

        var token = logIn.logIn("Alice", "wonderland123");

        assertThat(token.value()).isEqualTo("token-for-" + ALICE.id().value());
        assertThat(token.expiresAt()).isEqualTo(FakeTokenIssuer.EXPIRES_AT);
    }

    @Test
    void refusesAWrongPassword() {
        users.add(ALICE);

        assertThatExceptionOfType(InvalidCredentialsException.class).isThrownBy(() -> logIn.logIn("alice", "wrong-password"));
    }

    @Test
    void refusesAnUnknownUserTheSameWayAndStillComparesAHash() {
        assertThatExceptionOfType(InvalidCredentialsException.class).isThrownBy(() -> logIn.logIn("nobody", "whatever1"));
        // Comparing against a dummy hash makes an unknown user take as long as a wrong password.
        assertThat(hasher.comparisons).isEqualTo(1);
    }

    @Test
    void treatsAMalformedUsernameAsUnknownRatherThanExplainingTheRules() {
        assertThatExceptionOfType(InvalidCredentialsException.class).isThrownBy(() -> logIn.logIn("a b", "whatever1"));
        assertThat(hasher.comparisons).isEqualTo(1);
    }

    @Test
    void refusesAPasswordLongerThanBCryptReads() {
        users.add(ALICE);
        // BCrypt ignores everything after 72 bytes; accepting more would make a longer password silently equal a shorter one.
        var tooLong = "é".repeat(37);

        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> logIn.logIn("alice", tooLong))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("password"));
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<Username, User> byUsername = new HashMap<>();

        void add(User user) {
            byUsername.put(user.username(), user);
        }

        @Override
        public Optional<User> findByUsername(Username username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public boolean exists(UserId id) {
            return byUsername.values().stream().anyMatch(user -> user.id().equals(id));
        }
    }

    private static final class CountingPasswordHasher implements PasswordHasher {

        int comparisons;

        @Override
        public boolean matches(String rawPassword, String hash) {
            comparisons++;
            return hash.equals("hash:" + rawPassword);
        }

        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }
    }

    private enum FakeTokenIssuer implements TokenIssuer {
        INSTANCE;

        static final Instant EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");

        @Override
        public AccessToken issue(User user) {
            return new AccessToken("token-for-" + user.id().value(), EXPIRES_AT);
        }
    }
}
