package com.lucasamaral.pokedex.application.user;

public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException() {
        super("That username is already taken");
    }
}
