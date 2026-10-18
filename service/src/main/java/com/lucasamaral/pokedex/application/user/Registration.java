package com.lucasamaral.pokedex.application.user;

/** What a person sends to create an account. */
public record Registration(String username, String email, String password) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "Registration[username=" + username + ", email=" + email + "]";
    }
}
