package com.example.tasks.entity;

import com.example.tasks.entity.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Value Object: 3 to 30 lowercase letters, digits, dots, hyphens or underscores; compared case-insensitively. */
public record Username(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9._-]{3,30}");

    public Username {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidFieldException("username", "must be 3 to 30 lowercase letters, digits, dots, hyphens or underscores");
        }
    }

    /** Static Factory: normalizes what a person typed before checking it. */
    public static Username of(String raw) {
        return new Username(raw == null ? null : raw.strip().toLowerCase(Locale.ROOT));
    }
}
