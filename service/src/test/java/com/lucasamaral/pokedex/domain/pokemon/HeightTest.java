package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HeightTest {

    @Test
    void convertsDecimetersToMeters() {
        assertThat(Height.ofDecimeters(7).meters()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(Height.ofDecimeters(20).meters()).isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Height.ofDecimeters(-1))
                .withMessageContaining("negative");
    }
}
