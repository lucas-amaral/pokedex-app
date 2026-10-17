package com.lucasamaral.pokedex.domain.pokemon;

import java.math.BigDecimal;

/**
 * Body mass of a Pokémon. PokeAPI reports it in hectograms (tenths of a kilogram).
 */
public record Weight(int hectograms) {

    private static final int HECTOGRAMS_SCALE = 1;

    public Weight {
        if (hectograms < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + hectograms + " hg");
        }
    }

    public static Weight ofHectograms(int hectograms) {
        return new Weight(hectograms);
    }

    public BigDecimal kilograms() {
        return BigDecimal.valueOf(hectograms, HECTOGRAMS_SCALE);
    }
}
