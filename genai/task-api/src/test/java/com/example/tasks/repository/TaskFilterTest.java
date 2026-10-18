package com.example.tasks.repository;

import com.example.tasks.entity.InvalidParameterException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskFilterTest {

    @Test
    void acceptsARangeOfOneDay() {
        var day = LocalDate.parse("2026-10-20");

        assertThat(new TaskFilter(null, day, day).dueFrom()).isEqualTo(day);
    }

    @Test
    void refusesARangeThatEndsBeforeItStarts() {
        assertThatExceptionOfType(InvalidParameterException.class)
                .isThrownBy(() -> new TaskFilter(null, LocalDate.parse("2026-10-21"), LocalDate.parse("2026-10-20")))
                .satisfies(exception -> assertThat(exception.parameter()).isEqualTo("dueTo"));
    }
}
