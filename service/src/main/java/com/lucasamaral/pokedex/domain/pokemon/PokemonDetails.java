package com.lucasamaral.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;
import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requirePositive;

/**
 * Everything shown on a Pokémon's detail page (US02).
 *
 * @param name        PokeAPI name of this Pokémon, which can be an alternate form ("raichu-alola")
 * @param speciesName species it belongs to ("raichu"), as named in its evolution chain
 * @param spriteUrl   small front sprite; {@code null} when PokeAPI has none
 * @param artworkUrl  official artwork; {@code null} when PokeAPI has none
 * @param category    species genus such as "Seed Pokémon"; {@code null} when PokeAPI has none
 * @param description English Pokédex entry; {@code null} when PokeAPI has none
 * @param defaultForm {@code false} for an alternate form (a partner, regional, Mega or Gigantamax form); its evolution
 *                    chain is still the species', since PokeAPI defines evolution per species
 */
public record PokemonDetails(
        int id,
        String name,
        String speciesName,
        String spriteUrl,
        String artworkUrl,
        String category,
        String description,
        Height height,
        Weight weight,
        List<String> types,
        List<Ability> abilities,
        List<BaseStat> stats,
        EvolutionNode evolutionChain,
        boolean defaultForm) {

    public PokemonDetails {
        requirePositive(id, "Pokémon id must be positive");
        requireNonBlank(name, "Pokémon name must not be blank");
        requireNonBlank(speciesName, "Pokémon species name must not be blank");
        Objects.requireNonNull(height, "Pokémon height is required");
        Objects.requireNonNull(weight, "Pokémon weight is required");
        types = List.copyOf(Objects.requireNonNull(types, "Pokémon types are required"));
        abilities = List.copyOf(Objects.requireNonNull(abilities, "Pokémon abilities are required"));
        stats = List.copyOf(Objects.requireNonNull(stats, "Pokémon stats are required"));
        Objects.requireNonNull(evolutionChain, "Pokémon evolution chain is required");
    }
}
