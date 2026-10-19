package com.lucasamaral.pokedex.domain.pokemon;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;

/**
 * How a Pokémon evolves into the next stage. Only the trigger is always present; the rest is {@code null} when that
 * evolution does not need it.
 *
 * @param trigger       PokeAPI trigger such as "level-up", "use-item" or "trade"
 * @param minLevel      level required
 * @param item          item to use (for example "water-stone")
 * @param heldItem      item the Pokémon must hold (for example "metal-coat" when traded)
 * @param minHappiness  friendship required (160 for Espeon and Umbreon)
 * @param timeOfDay     "day" or "night"
 * @param knownMove     a move the Pokémon must know
 * @param knownMoveType the type of a move it must know (Sylveon: "fairy")
 */
public record EvolutionCondition(
        String trigger,
        Integer minLevel,
        String item,
        String heldItem,
        Integer minHappiness,
        String timeOfDay,
        String knownMove,
        String knownMoveType) {

    public EvolutionCondition {
        requireNonBlank(trigger, "Evolution trigger must not be blank");
    }

    /** The common case: a level or an item, nothing else. */
    public EvolutionCondition(String trigger, Integer minLevel, String item) {
        this(trigger, minLevel, item, null, null, null, null, null);
    }
}
