package com.example.tasks.entity;

import java.util.Objects;
import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "value is required");
    }

    static TaskId random() {
        return new TaskId(UUID.randomUUID());
    }
}
