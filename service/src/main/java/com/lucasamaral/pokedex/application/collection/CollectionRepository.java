package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port: where the collection is stored (PostgreSQL in production).
 */
public interface CollectionRepository {

    Optional<CollectedPokemon> findById(UUID id);

    Optional<CollectedPokemon> findByPokeapiId(int pokeapiId);

    /** Inserts a new Pokémon or replaces the stored one with the same id. */
    CollectedPokemon save(CollectedPokemon pokemon);

    /** The matching Pokémon in Pokédex order. */
    Page<CollectedPokemon> findPage(CollectionFilter filter, PageQuery query);
}
