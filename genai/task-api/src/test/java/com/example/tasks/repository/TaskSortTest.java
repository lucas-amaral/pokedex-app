package com.example.tasks.repository;

import com.example.tasks.entity.InvalidParameterException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TaskSortTest {

    @Test
    void defaultsToDueDateAscending() {
        assertThat(TaskSort.parse(null)).isEqualTo(new TaskSort(TaskSort.Field.DUE_DATE, TaskSort.Direction.ASC));
        assertThat(TaskSort.parse("  ")).isEqualTo(TaskSort.DEFAULT);
    }

    @Test
    void readsAFieldAndAnOptionalDirection() {
        assertThat(TaskSort.parse("title,desc")).isEqualTo(new TaskSort(TaskSort.Field.TITLE, TaskSort.Direction.DESC));
        assertThat(TaskSort.parse("createdAt")).isEqualTo(new TaskSort(TaskSort.Field.CREATED_AT, TaskSort.Direction.ASC));
        assertThat(TaskSort.parse("status,ASC")).isEqualTo(new TaskSort(TaskSort.Field.STATUS, TaskSort.Direction.ASC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "ownerId", "title,up", "title,asc,extra", ",asc", "description"})
    void refusesAnythingElse(String raw) {
        assertThatExceptionOfType(InvalidParameterException.class)
                .isThrownBy(() -> TaskSort.parse(raw))
                .satisfies(exception -> {
                    assertThat(exception.parameter()).isEqualTo("sort");
                    assertThat(exception.getMessage()).contains("dueDate, createdAt, title, status");
                });
    }
}
