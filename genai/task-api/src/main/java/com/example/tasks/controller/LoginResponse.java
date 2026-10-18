package com.example.tasks.controller;

import com.example.tasks.service.AccessToken;

import java.time.Instant;

/** Send {@code accessToken} as "Authorization: Bearer ..." until {@code expiresAt}. */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {

    static LoginResponse from(AccessToken token) {
        return new LoginResponse(token.value(), "Bearer", token.expiresAt());
    }
}
