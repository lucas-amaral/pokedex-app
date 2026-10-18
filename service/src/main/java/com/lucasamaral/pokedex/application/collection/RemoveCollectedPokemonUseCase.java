package com.lucasamaral.pokedex.application.collection;

import java.util.UUID;

/**
 * Input port: take a Pokémon out of the collection.
 */
public interface RemoveCollectedPokemonUseCase {

    /**
     * @throws CollectedPokemonNotFoundException when the Pokémon is not in the collection
     */
    void remove(UUID id);
}
