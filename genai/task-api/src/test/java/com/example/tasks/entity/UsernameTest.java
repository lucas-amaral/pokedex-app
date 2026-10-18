package com.example.tasks.entity;

import com.example.tasks.entity.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UsernameTest {

    @Test
    void isTrimmedAndLowercased() {
        assertThat(Username.of("  Alice ").value()).isEqualTo("alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "with space", "émile", "semi;colon", "a234567890123456789012345678901"})
    void refusesAnythingButThreeToThirtySimpleCharacters(String raw) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> Username.of(raw))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("username"));
    }

    @Test
    void refusesNull() {
        assertThatExceptionOfType(InvalidFieldException.class).isThrownBy(() -> Username.of(null));
    }
}
