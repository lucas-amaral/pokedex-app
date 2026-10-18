package com.example.tasks.entity;

import java.util.Optional;

/** Value Object: free text of up to 2,000 characters; line breaks and tabs are kept. */
public record TaskDescription(String value) {

    public static final int MAX_LENGTH = 2000;

    public TaskDescription {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("a description has text; an absent one is no description");
        }
        Text.requireMaxLength("description", value, MAX_LENGTH);
        Text.requireNoControlCharactersButLineBreaksAndTabs("description", value);
    }

    /** Static Factory: empty when the client sent nothing or only spaces. */
    public static Optional<TaskDescription> of(String raw) {
        return Optional.ofNullable(Text.trimmedOrNull(raw)).map(TaskDescription::new);
    }
}
