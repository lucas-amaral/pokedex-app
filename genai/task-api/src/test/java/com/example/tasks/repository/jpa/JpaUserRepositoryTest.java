package com.example.tasks.repository.jpa;

import com.example.tasks.entity.UserId;
import com.example.tasks.entity.Username;
import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Against real PostgreSQL with the real migrations, including the demo users. */
@DataJpaTest
@Import({PostgresTestConfiguration.class, JpaUserRepository.class})
class JpaUserRepositoryTest {

    @Autowired
    private JpaUserRepository credentials;

    @Test
    void findsAUserByUsername() {
        var alice = credentials.findByUsername(Username.of("alice")).orElseThrow();

        assertThat(alice.username().value()).isEqualTo("alice");
        assertThat(alice.passwordHash()).startsWith("$2");
    }

    @Test
    void findsNothingForAnUnknownUsername() {
        assertThat(credentials.findByUsername(Username.of("mallory"))).isEmpty();
    }

    @Test
    void tellsWhetherAUserStillExists() {
        var alice = credentials.findByUsername(Username.of("alice")).orElseThrow();

        assertThat(credentials.exists(alice.id())).isTrue();
        assertThat(credentials.exists(new UserId(UUID.randomUUID()))).isFalse();
    }
}
