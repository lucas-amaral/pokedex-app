package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.user.EmailTakenException;
import com.lucasamaral.pokedex.application.user.UserRepository;
import com.lucasamaral.pokedex.application.user.UsernameTakenException;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: user accounts in PostgreSQL. A unique-key violation (two sign-ups with the same name at once)
 * becomes the same "taken" error the use case raises when it sees the duplicate first.
 */
@Repository
@Transactional(readOnly = true)
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(JpaUserRepository::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(JpaUserRepository::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpa.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public User save(User user) {
        var entity = new UserEntity();
        entity.id = user.id();
        entity.username = user.username().value();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.createdAt = user.createdAt();
        try {
            return toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            var constraint = e.getCause() instanceof ConstraintViolationException violation ? violation.getConstraintName() : "";
            if ("uk_users_username".equalsIgnoreCase(constraint)) {
                throw new UsernameTakenException();
            }
            if ("uk_users_email".equalsIgnoreCase(constraint)) {
                throw new EmailTakenException();
            }
            throw e;
        }
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.id, Username.of(entity.username), Email.of(entity.email), entity.passwordHash,
                entity.role, entity.createdAt);
    }
}
