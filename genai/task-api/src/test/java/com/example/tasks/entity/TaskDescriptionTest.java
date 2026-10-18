package com.example.tasks.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskDescriptionTest {

    @Test
    void isOptional() {
        assertThat(TaskDescription.of(null)).isEmpty();
        assertThat(TaskDescription.of("   ")).isEmpty();
    }

    @Test
    void keepsLineBreaksAndTabs() {
        assertThat(TaskDescription.of(" 2 liters\n\tskimmed ")).hasValue(new TaskDescription("2 liters\n\tskimmed"));
    }

    @Test
    void refusesOtherControlCharacters() {
        // PostgreSQL cannot store a NUL character in text.
        assertInvalidDescription("milk\u0000");
    }

    @Test
    void refusesMoreThanTwoThousandCharacters() {
        assertThat(TaskDescription.of("d".repeat(2000))).isPresent();
        assertInvalidDescription("d".repeat(2001));
    }

    private static void assertInvalidDescription(String raw) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> TaskDescription.of(raw))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("description"));
    }
}
