package com.example.tasks.controller;

import com.example.tasks.service.TaskChanges;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Replaces title, description and due date; a field left out is cleared. Status changes through
 * {@code PATCH /{id}/status}, so it is not accepted here.
 */
public record UpdateTaskRequest(
        @Schema(description = "The version you read", example = "0") @NotNull @PositiveOrZero Long version,
        @Schema(example = "Buy oat milk") String title,
        @Schema(example = "1 liter") String description,
        @Schema(example = "2030-01-31") LocalDate dueDate) {

    TaskChanges toChanges() {
        return new TaskChanges(version, title, description, dueDate);
    }
}
