package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.user.User;

/**
 * Output port: issues access tokens (signed JWTs in production).
 */
public interface TokenIssuer {

    AccessToken issue(User user);
}
