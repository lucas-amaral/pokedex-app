package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryResponse(
        int id,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<AbilityResponse> abilities) {

    public record AbilityResponse(String name, boolean hidden) {

        static AbilityResponse from(Ability ability) {
            return new AbilityResponse(ability.name(), ability.hidden());
        }
    }

    static PokemonSummaryResponse from(PokemonSummary summary) {
        return new PokemonSummaryResponse(
                summary.id(),
                summary.name(),
                summary.spriteUrl(),
                summary.category(),
                summary.weight().kilograms(),
                summary.abilities().stream().map(AbilityResponse::from).toList());
    }
}
