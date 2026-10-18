package com.lucasamaral.pokedex.application.user;

import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port: where users are stored (PostgreSQL in production).
 */
public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(Username username);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    User save(User user);
}
