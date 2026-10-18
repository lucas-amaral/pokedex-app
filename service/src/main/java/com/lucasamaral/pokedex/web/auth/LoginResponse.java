package com.lucasamaral.pokedex.web.auth;

import com.lucasamaral.pokedex.application.user.AuthenticatedSession;

import java.time.Instant;

/** Send {@code accessToken} as "Authorization: Bearer ..." until {@code expiresAt}. */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

    static LoginResponse from(AuthenticatedSession session) {
        return new LoginResponse(session.token().value(), "Bearer", session.token().expiresAt(), UserResponse.from(session.user()));
    }
}
