package com.lucasamaral.pokedex.domain.pokemon;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;

/**
 * One of a Pokémon's six base stats (hp, attack, defense, special-attack, special-defense, speed).
 */
public record BaseStat(String name, int value) {

    public static final int MAX_VALUE = 255;

    public BaseStat {
        requireNonBlank(name, "Stat name must not be blank");
        if (value < 1 || value > MAX_VALUE) {
            throw new IllegalArgumentException("Base stat must be between 1 and " + MAX_VALUE + ", was " + value);
        }
    }
}
