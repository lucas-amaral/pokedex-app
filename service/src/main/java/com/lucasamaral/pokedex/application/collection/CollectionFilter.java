package com.lucasamaral.pokedex.application.collection;

import java.util.Locale;

/**
 * Optional filters for the collection list. Values are trimmed and lowercased; blank means "no filter".
 *
 * @param name part of the Pokémon's name or nickname
 * @param tag  one of its tags, matched exactly
 */
public record CollectionFilter(String name, String tag) {

    private static final CollectionFilter NONE = new CollectionFilter(null, null);

    public CollectionFilter {
        name = normalize(name);
        tag = normalize(tag);
    }

    public static CollectionFilter none() {
        return NONE;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
