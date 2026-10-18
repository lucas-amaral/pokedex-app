package com.lucasamaral.pokedex.application.user;

/**
 * Output port: one-way password hashing (BCrypt in production).
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
