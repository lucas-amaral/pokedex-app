package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WeightTest {

    @Test
    void convertsHectogramsToKilograms() {
        assertThat(Weight.ofHectograms(69).kilograms()).isEqualByComparingTo(new BigDecimal("6.9"));
    }

    @Test
    void keepsWholeKilogramsExact() {
        assertThat(Weight.ofHectograms(90).kilograms()).isEqualByComparingTo(new BigDecimal("9"));
    }

    @Test
    void acceptsZero() {
        assertThat(Weight.ofHectograms(0).kilograms()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsNegativeWeight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Weight.ofHectograms(-1))
                .withMessageContaining("negative");
    }
}
