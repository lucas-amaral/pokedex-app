package com.lucasamaral.pokedex.application.user;

/**
 * Login failed. The message never says whether the username or the password was wrong,
 * so it cannot be used to find out which usernames exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Wrong username or password");
    }
}
