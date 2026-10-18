package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.InvalidFieldException;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class UserAccountService implements RegisterUserUseCase, AuthenticateUserUseCase, GetCurrentUserUseCase {

    static final int PASSWORD_MIN_LENGTH = 8;
    /** BCrypt only reads the first 72 bytes; a longer password would be silently cut. */
    static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;
    /** Checked against when the username is unknown, so both failures take about the same time. */
    private final String timingDummyHash;

    public UserAccountService(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer, Clock clock) {
        this.users = Objects.requireNonNull(users, "users is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher is required");
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer, "tokenIssuer is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.timingDummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public User register(Registration registration) {
        var username = Username.of(registration.username());
        var email = Email.of(registration.email());
        checkPassword(registration.password());
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException();
        }
        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        var hash = passwordHasher.hash(registration.password());
        // PostgreSQL keeps microseconds; truncating here makes the response match what a later read returns.
        return users.save(User.register(username, email, hash, clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }

    @Override
    public AuthenticatedSession authenticate(String username, String password) {
        var user = findUser(username);
        if (user == null) {
            passwordHasher.matches(String.valueOf(password), timingDummyHash);
            throw new InvalidCredentialsException();
        }
        if (password == null || !passwordHasher.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthenticatedSession(user, tokenIssuer.issue(user));
    }

    @Override
    public User currentUser(UUID id) {
        return users.findById(Objects.requireNonNull(id, "id is required")).orElseThrow(InvalidCredentialsException::new);
    }

    private User findUser(String rawUsername) {
        try {
            return users.findByUsername(Username.of(rawUsername)).orElse(null);
        } catch (InvalidFieldException malformed) {
            return null;
        }
    }

    private static void checkPassword(String password) {
        if (password == null || password.isBlank() || password.length() < PASSWORD_MIN_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new InvalidFieldException("password",
                    "must be " + PASSWORD_MIN_LENGTH + " to " + PASSWORD_MAX_BYTES + " characters and not only spaces");
        }
    }
}
