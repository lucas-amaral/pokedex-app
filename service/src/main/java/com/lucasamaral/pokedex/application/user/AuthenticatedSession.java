package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.user.User;

/** A successful login: who logged in and the token to send with later requests. */
public record AuthenticatedSession(User user, AccessToken token) {
}
