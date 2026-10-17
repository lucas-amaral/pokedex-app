package com.lucasamaral.pokedex.domain.pokemon;

/**
 * A Pokémon ability (the "skills" in the catalog). Hidden abilities are the rare ones.
 */
public record Ability(String name, boolean hidden) {

    public Ability {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ability name must not be blank");
        }
    }
}
