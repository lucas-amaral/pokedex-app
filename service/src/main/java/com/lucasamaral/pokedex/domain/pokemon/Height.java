package com.lucasamaral.pokedex.domain.pokemon;

import java.math.BigDecimal;

/**
 * Height of a Pokémon. PokeAPI reports it in decimeters (tenths of a meter).
 */
public record Height(int decimeters) {

    private static final int DECIMETERS_SCALE = 1;

    public Height {
        if (decimeters < 0) {
            throw new IllegalArgumentException("Height cannot be negative: " + decimeters + " dm");
        }
    }

    public static Height ofDecimeters(int decimeters) {
        return new Height(decimeters);
    }

    public BigDecimal meters() {
        return BigDecimal.valueOf(decimeters, DECIMETERS_SCALE);
    }
}
