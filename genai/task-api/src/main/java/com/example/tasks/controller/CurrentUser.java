package com.example.tasks.controller;

import com.example.tasks.entity.UserId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** The caller, as named by the subject of the token the security filter already verified. */
final class CurrentUser {

    private CurrentUser() {
    }

    static UserId from(Jwt token) {
        return new UserId(UUID.fromString(token.getSubject()));
    }
}
