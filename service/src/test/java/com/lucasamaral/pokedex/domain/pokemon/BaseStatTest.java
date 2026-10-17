package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BaseStatTest {

    @Test
    void keepsNameAndValue() {
        var stat = new BaseStat("special-attack", 65);

        assertThat(stat.name()).isEqualTo("special-attack");
        assertThat(stat.value()).isEqualTo(65);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, BaseStat.MAX_VALUE + 1})
    void rejectsValuesOutsideTheGameRange(int value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BaseStat("hp", value))
                .withMessageContaining("between");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BaseStat(" ", 45));
    }
}
