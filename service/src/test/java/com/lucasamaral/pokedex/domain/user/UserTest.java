package com.lucasamaral.pokedex.domain.user;

import com.lucasamaral.pokedex.domain.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    @Test
    void normalizesTheUsernameToLowercase() {
        assertThat(Username.of("  Ash.Ketchum_1 ").value()).isEqualTo("ash.ketchum_1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a-name-that-is-longer-than-thirty", "has space", "ünïcode", ""})
    void rejectsMalformedUsernames(String username) {
        assertInvalid("username", () -> Username.of(username));
    }

    @Test
    void normalizesTheEmailToLowercase() {
        assertThat(Email.of(" Ash@Pallet.Town ").value()).isEqualTo("ash@pallet.town");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ash", "ash@", "@pallet.town", "ash@pallet", "a b@pallet.town", ""})
    void rejectsMalformedEmails(String email) {
        assertInvalid("email", () -> Email.of(email));
    }

    @Test
    void rejectsAnEmailWithControlCharacters() {
        // PostgreSQL cannot store a NUL character in text; it must be refused before reaching the database.
        assertInvalid("email", () -> Email.of("br\u0000ock@pewter.city"));
        assertInvalid("email", () -> Email.of("brock@pewter\u0007.city"));
    }

    @Test
    void aNewUserIsARegularUser() {
        var ash = User.register(Username.of("ash"), Email.of("ash@pallet.town"), "hashed", NOW);

        assertThat(ash.id()).isNotNull();
        assertThat(ash.role()).isEqualTo(Role.USER);
        assertThat(ash.createdAt()).isEqualTo(NOW);
    }

    @Test
    void onlyAnAdminIsAnAdmin() {
        var ash = User.register(Username.of("ash"), Email.of("ash@pallet.town"), "hashed", NOW);
        var oak = new User(ash.id(), Username.of("oak"), Email.of("oak@pallet.town"), "hashed", Role.ADMIN, NOW);

        assertThat(ash.isAdmin()).isFalse();
        assertThat(oak.isAdmin()).isTrue();
    }

    @Test
    void neverShowsThePasswordHashWhenPrinted() {
        var ash = User.register(Username.of("ash"), Email.of("ash@pallet.town"), "$2a$10$secret", NOW);

        assertThat(ash.toString()).doesNotContain("$2a$10$secret");
    }

    private static void assertInvalid(String field, Runnable creation) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(creation::run)
                .satisfies(exception -> assertThat(exception.field()).isEqualTo(field));
    }
}
