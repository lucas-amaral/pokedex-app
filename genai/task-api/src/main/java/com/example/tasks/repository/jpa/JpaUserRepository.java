package com.example.tasks.repository.jpa;

import com.example.tasks.entity.User;
import com.example.tasks.entity.UserId;
import com.example.tasks.entity.Username;
import com.example.tasks.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adapter for {@link UserRepository} on the {@code users} table. */
@Component
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository users;

    JpaUserRepository(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return users.findByUsername(username.value())
                .map(entity -> new User(new UserId(entity.getId()), new Username(entity.getUsername()), entity.getPasswordHash()));
    }

    @Override
    public boolean exists(UserId id) {
        return users.existsById(id.value());
    }
}
