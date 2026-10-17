package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

/**
 * Input port for US02: the full detail view of one Pokémon.
 */
public interface GetPokemonDetailsUseCase {

    /**
     * @param idOrName national Pokédex number ("25") or PokeAPI name ("pikachu"), case-insensitive
     * @throws PokemonNotFoundException when no Pokémon matches
     */
    PokemonDetails getDetails(String idOrName);
}
