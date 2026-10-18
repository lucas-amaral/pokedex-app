package com.example.tasks.service;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Wrong username or password");
    }
}
