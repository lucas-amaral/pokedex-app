package com.example.tasks.controller;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A task as the API shows it; send {@code version} back with any change. */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static TaskResponse from(Task task) {
        return new TaskResponse(task.id().value(), task.title().value(),
                task.description() == null ? null : task.description().value(), task.status(), task.dueDate(),
                task.createdAt(), task.updatedAt(), task.version());
    }
}
