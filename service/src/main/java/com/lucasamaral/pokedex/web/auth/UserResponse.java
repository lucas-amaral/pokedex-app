package com.lucasamaral.pokedex.web.auth;

import com.lucasamaral.pokedex.domain.user.User;

import java.time.Instant;
import java.util.UUID;

/** A user as the API shows it: never the password hash. */
public record UserResponse(UUID id, String username, String email, String role, Instant createdAt) {

    static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username().value(), user.email().value(), user.role().name(), user.createdAt());
    }
}
