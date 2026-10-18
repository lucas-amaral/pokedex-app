package com.example.tasks.controller;

import com.example.tasks.entity.TaskStatus;
import com.example.tasks.service.StatusChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeStatusRequest(
        @Schema(description = "The version you read", example = "0") @NotNull @PositiveOrZero Long version,
        @Schema(example = "IN_PROGRESS") @NotNull TaskStatus status) {

    StatusChange toStatusChange() {
        return new StatusChange(version, status);
    }
}
