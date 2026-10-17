package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PokemonSummaryTest {

    private static final String SPRITE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png";
    private static final List<Ability> ABILITIES = List.of(new Ability("overgrow", false), new Ability("chlorophyll", true));

    @Test
    void exposesTheFieldsShownInTheCatalog() {
        var summary = new PokemonSummary(1, "bulbasaur", SPRITE, "Seed Pokémon", Weight.ofHectograms(69), ABILITIES);

        assertThat(summary.id()).isEqualTo(1);
        assertThat(summary.name()).isEqualTo("bulbasaur");
        assertThat(summary.spriteUrl()).isEqualTo(SPRITE);
        assertThat(summary.category()).isEqualTo("Seed Pokémon");
        assertThat(summary.weight()).isEqualTo(Weight.ofHectograms(69));
        assertThat(summary.abilities()).extracting(Ability::name).containsExactly("overgrow", "chlorophyll");
    }

    @Test
    void allowsMissingSpriteAndCategory() {
        var summary = new PokemonSummary(10001, "deoxys-attack", null, null, Weight.ofHectograms(608), ABILITIES);

        assertThat(summary.spriteUrl()).isNull();
        assertThat(summary.category()).isNull();
    }

    @Test
    void rejectsNonPositiveId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PokemonSummary(0, "bulbasaur", SPRITE, "Seed Pokémon", Weight.ofHectograms(69), ABILITIES))
                .withMessageContaining("id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void rejectsBlankName(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PokemonSummary(1, name, SPRITE, "Seed Pokémon", Weight.ofHectograms(69), ABILITIES))
                .withMessageContaining("name");
    }

    @Test
    void requiresWeight() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PokemonSummary(1, "bulbasaur", SPRITE, "Seed Pokémon", null, ABILITIES))
                .withMessageContaining("weight");
    }

    @Test
    void requiresAbilities() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PokemonSummary(1, "bulbasaur", SPRITE, "Seed Pokémon", Weight.ofHectograms(69), null))
                .withMessageContaining("abilities");
    }

    @Test
    void isNotAffectedByChangesToTheOriginalAbilityList() {
        var abilities = new ArrayList<>(ABILITIES);
        var summary = new PokemonSummary(1, "bulbasaur", SPRITE, "Seed Pokémon", Weight.ofHectograms(69), abilities);

        abilities.clear();

        assertThat(summary.abilities()).hasSize(2);
    }
}
