package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.math.BigDecimal;
import java.util.List;

public record PokemonDetailsResponse(
        int id,
        String name,
        String speciesName,
        String spriteUrl,
        String artworkUrl,
        String category,
        String description,
        BigDecimal heightM,
        BigDecimal weightKg,
        List<String> types,
        List<AbilityResponse> abilities,
        List<StatResponse> stats,
        EvolutionNodeResponse evolutionChain) {

    public record StatResponse(String name, int value) {

        public static StatResponse from(BaseStat stat) {
            return new StatResponse(stat.name(), stat.value());
        }
    }

    /**
     * @param condition how the previous stage evolves into this one; {@code null} for the base form
     */
    public record EvolutionNodeResponse(
            int speciesId,
            String name,
            String spriteUrl,
            ConditionResponse condition,
            List<EvolutionNodeResponse> evolvesTo) {

        static EvolutionNodeResponse from(EvolutionNode node) {
            return new EvolutionNodeResponse(
                    node.speciesId(),
                    node.name(),
                    node.spriteUrl(),
                    node.condition() == null ? null : ConditionResponse.from(node.condition()),
                    node.evolvesTo().stream().map(EvolutionNodeResponse::from).toList());
        }
    }

    public record ConditionResponse(String trigger, Integer minLevel, String item) {

        static ConditionResponse from(EvolutionCondition condition) {
            return new ConditionResponse(condition.trigger(), condition.minLevel(), condition.item());
        }
    }

    static PokemonDetailsResponse from(PokemonDetails details) {
        return new PokemonDetailsResponse(
                details.id(),
                details.name(),
                details.speciesName(),
                details.spriteUrl(),
                details.artworkUrl(),
                details.category(),
                details.description(),
                details.height().meters(),
                details.weight().kilograms(),
                details.types(),
                details.abilities().stream().map(AbilityResponse::from).toList(),
                details.stats().stream().map(StatResponse::from).toList(),
                EvolutionNodeResponse.from(details.evolutionChain()));
    }
}
