package com.example.tasks.service;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    /** Keeps the token out of logs. */
    @Override
    public String toString() {
        return "AccessToken[expiresAt=" + expiresAt + "]";
    }
}
