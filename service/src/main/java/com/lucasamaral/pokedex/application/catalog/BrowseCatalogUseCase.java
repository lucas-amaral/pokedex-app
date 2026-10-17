package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;

/**
 * Input port for US01: browse the Pokémon catalog one page at a time.
 */
public interface BrowseCatalogUseCase {

    Page<PokemonSummary> browse(PageQuery query);
}
