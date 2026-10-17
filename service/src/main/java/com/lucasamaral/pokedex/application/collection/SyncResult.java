package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

/**
 * Outcome of a sync: the stored Pokémon, and whether it was added ({@code true}) or refreshed ({@code false}).
 */
public record SyncResult(CollectedPokemon pokemon, boolean created) {
}
