package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.user.User;

import java.util.UUID;

/**
 * Input port: the user behind a valid token.
 */
public interface GetCurrentUserUseCase {

    /**
     * @throws InvalidCredentialsException when the account no longer exists
     */
    User currentUser(UUID id);
}
