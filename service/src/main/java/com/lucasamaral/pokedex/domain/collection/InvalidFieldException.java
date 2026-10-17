package com.lucasamaral.pokedex.domain.collection;

/**
 * A proprietary field breaks one of its rules. Carries the field name so the API can say which one.
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
