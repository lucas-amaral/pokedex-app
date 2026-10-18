package com.example.tasks.repository;

import com.example.tasks.entity.User;
import com.example.tasks.entity.UserId;
import com.example.tasks.entity.Username;

import java.util.Optional;

/** Output port: where users and their password hashes are stored. */
public interface UserRepository {

    Optional<User> findByUsername(Username username);

    /** Whether the account still exists; a token can outlive the account it was issued for. */
    boolean exists(UserId id);
}
