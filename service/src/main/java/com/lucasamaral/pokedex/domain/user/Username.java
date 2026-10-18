package com.lucasamaral.pokedex.domain.user;

import com.lucasamaral.pokedex.domain.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A login name: 3 to 30 lowercase letters, digits, dots, hyphens or underscores. Input is trimmed and lowercased,
 * so "Ash" and "ash" are the same user.
 */
public record Username(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9._-]{3,30}");

    public Username {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidFieldException("username", "must be 3 to 30 letters, digits, dots, hyphens or underscores");
        }
    }

    public static Username of(String raw) {
        return new Username(raw == null ? null : raw.strip().toLowerCase(Locale.ROOT));
    }
}
