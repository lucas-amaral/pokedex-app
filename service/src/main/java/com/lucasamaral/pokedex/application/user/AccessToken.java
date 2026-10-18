package com.lucasamaral.pokedex.application.user;

import java.time.Instant;

/** A signed token that proves who the caller is, until it expires. */
public record AccessToken(String value, Instant expiresAt) {
}
