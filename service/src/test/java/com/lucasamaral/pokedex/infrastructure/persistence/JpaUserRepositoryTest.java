package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.user.EmailTakenException;
import com.lucasamaral.pokedex.application.user.UserRepository;
import com.lucasamaral.pokedex.application.user.UsernameTakenException;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.Role;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;
import com.lucasamaral.pokedex.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestConfiguration.class, JpaUserRepository.class})
class JpaUserRepositoryTest {

    private static final Instant CREATED = Instant.parse("2026-10-18T10:00:00Z");

    @Autowired
    private UserRepository users;

    @Autowired
    private UserJpaRepository jpa;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void storesAndReadsBackAUser() {
        var ash = user("ash", "ash@pallet.town", Role.USER);

        users.save(ash);
        flushAndClear();

        assertThat(users.findById(ash.id())).contains(ash);
        assertThat(users.findByUsername(Username.of("ash"))).contains(ash);
        assertThat(users.findByUsername(Username.of("gary"))).isEmpty();
    }

    @Test
    void storesTheRole() {
        var oak = users.save(user("oak", "oak@pallet.town", Role.ADMIN));
        flushAndClear();

        assertThat(users.findById(oak.id()).orElseThrow().isAdmin()).isTrue();
    }

    @Test
    void knowsWhetherAUsernameOrEmailIsInUse() {
        users.save(user("ash", "ash@pallet.town", Role.USER));
        flushAndClear();

        assertThat(users.existsByUsername(Username.of("ash"))).isTrue();
        assertThat(users.existsByUsername(Username.of("misty"))).isFalse();
        assertThat(users.existsByEmail(Email.of("ash@pallet.town"))).isTrue();
        assertThat(users.existsByEmail(Email.of("misty@cerulean.city"))).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesASecondAccountWithTheSameUsername() {
        users.save(user("ash", "ash@pallet.town", Role.USER));
        try {
            assertThatExceptionOfType(UsernameTakenException.class)
                    .isThrownBy(() -> users.save(user("ash", "other@pallet.town", Role.USER)));
        } finally {
            jpa.deleteAll();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesASecondAccountWithTheSameEmail() {
        users.save(user("ash", "ash@pallet.town", Role.USER));
        try {
            assertThatExceptionOfType(EmailTakenException.class)
                    .isThrownBy(() -> users.save(user("misty", "ash@pallet.town", Role.USER)));
        } finally {
            jpa.deleteAll();
        }
    }

    private static User user(String username, String email, Role role) {
        return new User(UUID.randomUUID(), Username.of(username), Email.of(email), "$2a$10$hash", role, CREATED);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
