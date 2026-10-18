package com.example.tasks.service;

/** Output port: one-way password hashing. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
