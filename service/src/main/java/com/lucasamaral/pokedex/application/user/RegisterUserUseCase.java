package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.user.User;

/**
 * Input port: create an account.
 */
public interface RegisterUserUseCase {

    /**
     * @throws com.lucasamaral.pokedex.domain.InvalidFieldException for a malformed username, email or password
     * @throws UsernameTakenException                                 when the username is in use
     * @throws EmailTakenException                                    when the email is in use
     */
    User register(Registration registration);
}
