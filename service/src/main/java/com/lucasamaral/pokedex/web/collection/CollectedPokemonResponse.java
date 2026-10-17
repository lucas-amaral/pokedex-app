package com.lucasamaral.pokedex.web.collection;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.web.catalog.AbilityResponse;
import com.lucasamaral.pokedex.web.catalog.PokemonDetailsResponse.StatResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A collected Pokémon: the source data copied from PokeAPI, then the proprietary fields.
 */
public record CollectedPokemonResponse(
        UUID id,
        int pokeapiId,
        String name,
        String speciesName,
        String category,
        String description,
        String spriteUrl,
        String artworkUrl,
        BigDecimal heightM,
        BigDecimal weightKg,
        List<String> types,
        List<AbilityResponse> abilities,
        List<StatResponse> stats,
        String nickname,
        String notes,
        String region,
        Map<String, String> localizedNames,
        Set<String> tags,
        Instant syncedAt) {

    static CollectedPokemonResponse from(CollectedPokemon pokemon) {
        var source = pokemon.source();
        var custom = pokemon.custom();
        return new CollectedPokemonResponse(
                pokemon.id(),
                source.pokeapiId(),
                source.name(),
                source.speciesName(),
                source.category(),
                source.description(),
                source.spriteUrl(),
                source.artworkUrl(),
                source.height().meters(),
                source.weight().kilograms(),
                source.types(),
                source.abilities().stream().map(AbilityResponse::from).toList(),
                source.stats().stream().map(StatResponse::from).toList(),
                custom.nickname(),
                custom.notes(),
                custom.region(),
                custom.localizedNames(),
                custom.tags(),
                pokemon.syncedAt());
    }
}
