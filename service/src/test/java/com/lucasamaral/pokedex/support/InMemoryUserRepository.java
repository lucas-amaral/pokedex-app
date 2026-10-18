package com.lucasamaral.pokedex.support;

import com.lucasamaral.pokedex.application.user.UserRepository;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hand-written fake of the user repository. */
public final class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> users = new LinkedHashMap<>();

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return users.values().stream().filter(user -> user.username().equals(username)).findFirst();
    }

    @Override
    public boolean existsByUsername(Username username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return users.values().stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }

    public List<User> all() {
        return List.copyOf(users.values());
    }
}
