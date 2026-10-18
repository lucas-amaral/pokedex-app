package com.lucasamaral.pokedex.application.user;

public class EmailTakenException extends RuntimeException {

    public EmailTakenException() {
        super("An account with that email already exists");
    }
}
