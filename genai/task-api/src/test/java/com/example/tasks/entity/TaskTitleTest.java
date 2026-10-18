package com.example.tasks.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskTitleTest {

    @Test
    void isTrimmed() {
        assertThat(TaskTitle.of("  Buy milk ").value()).isEqualTo("Buy milk");
    }

    @Test
    void acceptsOneHundredCharacters() {
        assertThat(TaskTitle.of("t".repeat(100)).value()).hasSize(100);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "Buy\nmilk", "Buy\u0000milk"})
    void refusesMissingBlankOrMultiLineTitles(String raw) {
        assertInvalidTitle(raw);
    }

    @Test
    void refusesMoreThanOneHundredCharacters() {
        assertInvalidTitle("t".repeat(101));
    }

    private static void assertInvalidTitle(String raw) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(() -> TaskTitle.of(raw))
                .satisfies(exception -> assertThat(exception.field()).isEqualTo("title"));
    }
}
