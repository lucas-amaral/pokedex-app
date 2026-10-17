package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;

/**
 * Output port: the external source of Pokémon data (PokeAPI in production).
 */
public interface PokemonCatalog {

    Page<PokemonSummary> findSummaries(PageQuery query);
}
