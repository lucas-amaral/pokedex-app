package com.lucasamaral.pokedex.domain.user;

import com.lucasamaral.pokedex.domain.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An email address, trimmed and lowercased. The check is deliberately simple (something@domain.tld):
 * only sending a message could prove an address is real.
 */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public Email {
        if (value == null || value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidFieldException("email", "must be a valid email address");
        }
    }

    public static Email of(String raw) {
        return new Email(raw == null ? null : raw.strip().toLowerCase(Locale.ROOT));
    }
}
