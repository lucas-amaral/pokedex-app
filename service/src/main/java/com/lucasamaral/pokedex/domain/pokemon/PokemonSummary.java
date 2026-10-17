package com.lucasamaral.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;

/**
 * The slice of a Pokémon shown in the catalog list (US01).
 *
 * @param id        national Pokédex number, as used by PokeAPI
 * @param spriteUrl front sprite; {@code null} when PokeAPI has none (some alternate forms)
 * @param category  species genus such as "Seed Pokémon"; {@code null} when PokeAPI has none
 */
public record PokemonSummary(
        int id,
        String name,
        String spriteUrl,
        String category,
        Weight weight,
        List<Ability> abilities) {

    public PokemonSummary {
        if (id <= 0) {
            throw new IllegalArgumentException("Pokémon id must be positive: " + id);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pokémon name must not be blank");
        }
        Objects.requireNonNull(weight, "Pokémon weight is required");
        abilities = List.copyOf(Objects.requireNonNull(abilities, "Pokémon abilities are required"));
    }
}
