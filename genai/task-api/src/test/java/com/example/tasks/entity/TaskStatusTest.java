package com.example.tasks.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** The transition table agreed before implementation, one row per pair of different statuses. */
class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "TODO,        IN_PROGRESS, true",
            "TODO,        DONE,        true",
            "IN_PROGRESS, TODO,        true",
            "IN_PROGRESS, DONE,        true",
            "DONE,        IN_PROGRESS, true",
            "DONE,        TODO,        false"})
    void allowsOnlyTheAgreedTransitions(TaskStatus from, TaskStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
