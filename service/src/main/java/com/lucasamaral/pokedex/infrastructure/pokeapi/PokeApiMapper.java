package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.util.Comparator;
import java.util.List;

/**
 * Translates PokeAPI's JSON shapes into domain objects.
 */
final class PokeApiMapper {

    private static final String ENGLISH = "en";

    private PokeApiMapper() {
    }

    static PokemonSummary toSummary(PokeApiResponses.Pokemon pokemon, PokeApiResponses.Species species) {
        return new PokemonSummary(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites() == null ? null : pokemon.sprites().frontDefault(),
                englishGenus(species),
                Weight.ofHectograms(pokemon.weight()),
                abilities(pokemon));
    }

    private static String englishGenus(PokeApiResponses.Species species) {
        if (species.genera() == null) {
            return null;
        }
        return species.genera().stream()
                .filter(genus -> ENGLISH.equals(genus.language().name()))
                .map(PokeApiResponses.Genus::genus)
                .findFirst()
                .orElse(null);
    }

    private static List<Ability> abilities(PokeApiResponses.Pokemon pokemon) {
        if (pokemon.abilities() == null) {
            return List.of();
        }
        return pokemon.abilities().stream()
                .sorted(Comparator.comparingInt(PokeApiResponses.PokemonAbility::slot))
                .map(entry -> new Ability(entry.ability().name(), entry.hidden()))
                .toList();
    }
}
