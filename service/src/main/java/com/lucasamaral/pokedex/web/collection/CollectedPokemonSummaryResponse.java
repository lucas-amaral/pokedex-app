package com.lucasamaral.pokedex.web.collection;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One row of the collection list. */
public record CollectedPokemonSummaryResponse(
        UUID id,
        int pokeapiId,
        String name,
        String nickname,
        String spriteUrl,
        String category,
        List<String> types,
        Set<String> tags,
        Instant syncedAt) {

    static CollectedPokemonSummaryResponse from(CollectedPokemon pokemon) {
        var source = pokemon.source();
        return new CollectedPokemonSummaryResponse(
                pokemon.id(),
                source.pokeapiId(),
                source.name(),
                pokemon.custom().nickname(),
                source.spriteUrl(),
                source.category(),
                source.types(),
                pokemon.custom().tags(),
                pokemon.syncedAt());
    }
}
