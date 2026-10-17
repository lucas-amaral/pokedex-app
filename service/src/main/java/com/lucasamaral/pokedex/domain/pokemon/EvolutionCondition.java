package com.lucasamaral.pokedex.domain.pokemon;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;

/**
 * How a Pokémon evolves into the next stage.
 *
 * @param trigger  PokeAPI trigger such as "level-up", "use-item" or "trade"
 * @param minLevel level required, or {@code null} when the trigger does not need one
 * @param item     item required (for example "water-stone"), or {@code null}
 */
public record EvolutionCondition(String trigger, Integer minLevel, String item) {

    public EvolutionCondition {
        requireNonBlank(trigger, "Evolution trigger must not be blank");
    }
}
