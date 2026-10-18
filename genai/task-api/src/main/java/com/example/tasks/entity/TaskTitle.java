package com.example.tasks.entity;

/** Value Object: a required, single-line title of 1 to 100 characters. */
public record TaskTitle(String value) {

    public static final int MAX_LENGTH = 100;

    public TaskTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("title", "must not be blank");
        }
        Text.requireMaxLength("title", value, MAX_LENGTH);
        Text.requireSingleLine("title", value);
    }

    /** Static Factory: trims what the client sent before checking it. */
    public static TaskTitle of(String raw) {
        return new TaskTitle(Text.trimmedOrNull(raw));
    }
}
