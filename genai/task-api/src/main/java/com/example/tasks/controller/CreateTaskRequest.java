package com.example.tasks.controller;

import com.example.tasks.service.NewTask;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Only what a client may set; the rules are the entity's, so there is one place to read them. */
public record CreateTaskRequest(
        @Schema(example = "Buy milk", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "2 liters") String description,
        @Schema(example = "2030-01-31") LocalDate dueDate) {

    NewTask toNewTask() {
        return new NewTask(title, description, dueDate);
    }
}
