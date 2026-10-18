package com.example.tasks.service;

import com.example.tasks.entity.InvalidFieldException;
import com.example.tasks.entity.User;
import com.example.tasks.entity.Username;
import com.example.tasks.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LogInService implements LogInUseCase {

    /** BCrypt reads only the first 72 bytes of a password. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    /** Compared against when the user is unknown, so that case takes as long as a wrong password. */
    private final String dummyHash;

    public LogInService(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.users = Objects.requireNonNull(users, "users is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher is required");
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer, "tokenIssuer is required");
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public AccessToken logIn(String username, String password) {
        requireCheckablePassword(password);
        var user = findUser(username);
        if (user.isEmpty()) {
            passwordHasher.matches(password, dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(password, user.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issue(user.get());
    }

    private static void requireCheckablePassword(String password) {
        if (password == null) {
            throw new InvalidFieldException("password", "must not be blank");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidFieldException("password", "must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }

    /** A malformed username cannot belong to anyone; saying so would explain the rules to someone guessing. */
    private Optional<User> findUser(String username) {
        try {
            return users.findByUsername(Username.of(username));
        } catch (InvalidFieldException malformed) {
            return Optional.empty();
        }
    }
}
