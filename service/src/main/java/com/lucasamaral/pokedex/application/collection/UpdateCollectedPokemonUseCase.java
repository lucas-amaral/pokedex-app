package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;

import java.util.UUID;

/**
 * Input port for US04: replace the proprietary fields of a collected Pokémon.
 */
public interface UpdateCollectedPokemonUseCase {

    /**
     * @param expectedVersion the version the caller read; the edit is refused if the Pokémon changed since
     * @throws CollectedPokemonNotFoundException        when the Pokémon is not in the collection
     * @throws CollectedPokemonVersionConflictException when the Pokémon changed since {@code expectedVersion}
     */
    CollectedPokemon update(UUID id, long expectedVersion, CustomFields fields);
}
