package com.lucasamaral.pokedex.domain.pokemon;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;

/**
 * A Pokémon ability (the "skills" in the catalog). Hidden abilities are the rare ones.
 */
public record Ability(String name, boolean hidden) {

    public Ability {
        requireNonBlank(name, "Ability name must not be blank");
    }
}
