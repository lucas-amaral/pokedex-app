package com.lucasamaral.pokedex.web.error;

import java.util.Set;

/**
 * The request body has fields the endpoint does not accept, for example source data a client tries to edit.
 */
public class UnknownFieldsException extends RuntimeException {

    private final Set<String> fields;

    public UnknownFieldsException(Set<String> fields) {
        super("Fields not accepted: " + fields);
        this.fields = Set.copyOf(fields);
    }

    public Set<String> fields() {
        return fields;
    }
}
