package com.lucasamaral.pokedex.application.collection;

import java.util.UUID;

/**
 * No Pokémon with that id is in the collection.
 */
public class CollectedPokemonNotFoundException extends RuntimeException {

    public CollectedPokemonNotFoundException(UUID id) {
        super("No Pokémon with id '" + id + "' in the collection");
    }
}
