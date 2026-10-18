package com.example.tasks.service;

/** Input port: exchange a username and password for an access token. */
public interface LogInUseCase {

    /**
     * @throws InvalidCredentialsException when the username is unknown or the password is wrong (never says which)
     * @throws com.example.tasks.entity.InvalidFieldException when the password is longer than BCrypt can check
     */
    AccessToken logIn(String username, String password);
}
