package com.lucasamaral.pokedex.application.collection;

import java.util.Locale;

/**
 * Optional filters for the collection list. Values are trimmed and lowercased; blank means "no filter".
 *
 * @param name      part of the Pokémon's name, nickname or one of its localized names
 * @param tag       one of its tags, matched exactly
 * @param pokeapiId its PokeAPI number, matched exactly (tells whether a catalog Pokémon is already collected)
 */
public record CollectionFilter(String name, String tag, Integer pokeapiId) {

    private static final CollectionFilter NONE = new CollectionFilter(null, null, null);

    public CollectionFilter {
        name = normalize(name);
        tag = normalize(tag);
        if (pokeapiId != null && pokeapiId <= 0) {
            throw new IllegalArgumentException("pokeapiId must be positive, was " + pokeapiId);
        }
    }

    public CollectionFilter(String name, String tag) {
        this(name, tag, null);
    }

    public static CollectionFilter none() {
        return NONE;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip().toLowerCase(Locale.ROOT);
    }
}
