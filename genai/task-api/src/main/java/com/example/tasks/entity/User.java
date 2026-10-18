package com.example.tasks.entity;

import java.util.Objects;

/** The existing user the brief assumes: enough to own tasks and to log in. */
public record User(UserId id, Username username, String passwordHash) {

    public User {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
    }

    /** Keeps the hash out of logs. */
    @Override
    public String toString() {
        return "User[id=" + id.value() + ", username=" + username.value() + "]";
    }
}
