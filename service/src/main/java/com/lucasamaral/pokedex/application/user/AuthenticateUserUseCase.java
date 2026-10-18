package com.lucasamaral.pokedex.application.user;

/**
 * Input port: log in with username and password.
 */
public interface AuthenticateUserUseCase {

    /**
     * @throws InvalidCredentialsException when the username or the password is wrong
     */
    AuthenticatedSession authenticate(String username, String password);
}
