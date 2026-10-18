package com.lucasamaral.pokedex.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A registered user. Only the password hash is kept, never the password.
 */
public record User(UUID id, Username username, Email email, String passwordHash, Role role, Instant createdAt) {

    public User {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        Objects.requireNonNull(role, "role is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }

    /** A new account; self-registered users are always regular users. */
    public static User register(Username username, Email email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), username, email, passwordHash, Role.USER, now);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Keeps the hash out of logs and error messages. */
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username.value() + ", role=" + role + "]";
    }
}
