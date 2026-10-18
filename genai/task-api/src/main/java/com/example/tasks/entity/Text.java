package com.example.tasks.entity;

/** Rules shared by the text fields of an entity. PostgreSQL cannot store a NUL character, so control characters are refused here. */
final class Text {

    private Text() {
    }

    /** Trimmed; {@code null} when absent or blank. */
    static String trimmedOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.strip();
    }

    static void requireMaxLength(String field, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new InvalidFieldException(field, "must be at most " + maxLength + " characters");
        }
    }

    static void requireSingleLine(String field, String value) {
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidFieldException(field, "must be a single line without control characters");
        }
    }

    static void requireNoControlCharactersButLineBreaksAndTabs(String field, String value) {
        if (value.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
            throw new InvalidFieldException(field, "must not contain control characters other than line breaks and tabs");
        }
    }
}
