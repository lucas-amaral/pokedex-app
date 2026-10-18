package com.lucasamaral.pokedex.application.seed;

import com.lucasamaral.pokedex.domain.user.Role;

/** An account created for demos, with a password everyone knows. */
public record DemoAccount(String username, String email, String password, Role role) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "DemoAccount[username=" + username + ", role=" + role + "]";
    }
}
