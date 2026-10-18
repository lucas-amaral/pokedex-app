package com.lucasamaral.pokedex.domain;

/**
 * A field breaks one of its rules (a proprietary field of a Pokémon, a username, a password). Carries the field name so the API can say which one.
 */
public class InvalidFieldException extends IllegalArgumentException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(field + " " + message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
