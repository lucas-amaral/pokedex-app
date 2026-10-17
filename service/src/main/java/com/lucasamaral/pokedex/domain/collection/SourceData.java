package com.lucasamaral.pokedex.domain.collection;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.util.List;
import java.util.Objects;

/**
 * The part of a collected Pokémon copied from PokeAPI. Users cannot edit it; a re-sync replaces it.
 * The evolution chain is not copied: it is read live from PokeAPI.
 */
public record SourceData(
        int pokeapiId,
        String name,
        String speciesName,
        String category,
        String description,
        String spriteUrl,
        String artworkUrl,
        Height height,
        Weight weight,
        List<String> types,
        List<Ability> abilities,
        List<BaseStat> stats) {

    public SourceData {
        if (pokeapiId <= 0) {
            throw new IllegalArgumentException("PokeAPI id must be positive: " + pokeapiId);
        }
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(speciesName, "speciesName is required");
        Objects.requireNonNull(height, "height is required");
        Objects.requireNonNull(weight, "weight is required");
        types = List.copyOf(Objects.requireNonNull(types, "types are required"));
        abilities = List.copyOf(Objects.requireNonNull(abilities, "abilities are required"));
        stats = List.copyOf(Objects.requireNonNull(stats, "stats are required"));
    }

    public static SourceData from(PokemonDetails details) {
        return new SourceData(
                details.id(),
                details.name(),
                details.speciesName(),
                details.category(),
                details.description(),
                details.spriteUrl(),
                details.artworkUrl(),
                details.height(),
                details.weight(),
                details.types(),
                details.abilities(),
                details.stats());
    }
}
