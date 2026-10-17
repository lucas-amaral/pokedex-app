package com.lucasamaral.pokedex.application.catalog;

import java.util.Locale;
import java.util.Objects;

/**
 * How a Pokémon is asked for: Pokédex number ("25") or PokeAPI name ("pikachu").
 */
public final class PokemonKey {

    private PokemonKey() {
    }

    /** Trimmed and lowercased, so "  Pikachu " and "pikachu" are the same request (and the same cache entry). */
    public static String normalize(String idOrName) {
        Objects.requireNonNull(idOrName, "idOrName is required");
        var key = idOrName.strip().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("idOrName must not be blank");
        }
        return key;
    }
}
