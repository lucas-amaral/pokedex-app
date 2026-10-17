package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;

import java.util.Objects;

public class BrowseCatalogService implements BrowseCatalogUseCase {

    private final PokemonCatalog catalog;

    public BrowseCatalogService(PokemonCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public Page<PokemonSummary> browse(PageQuery query) {
        Objects.requireNonNull(query, "query is required");
        return catalog.findSummaries(query);
    }
}
