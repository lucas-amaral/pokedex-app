package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AbilityTest {

    @Test
    void keepsNameAndHiddenFlag() {
        var ability = new Ability("chlorophyll", true);

        assertThat(ability.name()).isEqualTo("chlorophyll");
        assertThat(ability.hidden()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankName(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Ability(name, false))
                .withMessageContaining("name");
    }
}
