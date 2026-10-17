package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.util.UUID;

/**
 * Input port: read the local collection.
 */
public interface BrowseCollectionUseCase {

    Page<CollectedPokemon> list(CollectionFilter filter, PageQuery query);

    /**
     * @throws CollectedPokemonNotFoundException when no Pokémon with that id is in the collection
     */
    CollectedPokemon get(UUID id);
}
