package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.util.Optional;

/**
 * Output port: where the detail view comes from (PokeAPI in production).
 * <p>
 * Kept apart from {@link PokemonCatalog} so each use case depends only on what it uses.
 */
public interface PokemonDetailsCatalog {

    /**
     * @param idOrName lowercase PokeAPI name or Pokédex number
     * @return the Pokémon, or empty when it does not exist
     */
    Optional<PokemonDetails> findDetails(String idOrName);
}
