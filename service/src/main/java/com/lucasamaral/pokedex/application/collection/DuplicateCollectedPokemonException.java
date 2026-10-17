package com.lucasamaral.pokedex.application.collection;

/**
 * The collection already holds this Pokémon: another request stored it first.
 */
public class DuplicateCollectedPokemonException extends RuntimeException {

    public DuplicateCollectedPokemonException(int pokeapiId, Throwable cause) {
        super("Pokémon " + pokeapiId + " is already in the collection", cause);
    }
}
