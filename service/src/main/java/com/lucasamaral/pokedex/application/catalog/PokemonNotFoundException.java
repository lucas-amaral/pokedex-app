package com.lucasamaral.pokedex.application.catalog;

/**
 * No Pokémon matches the requested number or name.
 */
public class PokemonNotFoundException extends RuntimeException {

    public PokemonNotFoundException(String idOrName) {
        super("No Pokémon found for '" + idOrName + "'");
    }
}
