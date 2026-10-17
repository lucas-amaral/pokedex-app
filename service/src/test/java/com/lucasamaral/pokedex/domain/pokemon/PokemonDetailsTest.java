package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PokemonDetailsTest {

    @Test
    void exposesTheDetailView() {
        var details = aPokemonDetails().build();

        assertThat(details.id()).isEqualTo(1);
        assertThat(details.name()).isEqualTo("bulbasaur");
        assertThat(details.types()).containsExactly("grass", "poison");
        assertThat(details.stats()).extracting(BaseStat::name).startsWith("hp", "attack");
        assertThat(details.description()).startsWith("A strange seed");
        assertThat(details.evolutionChain().names()).containsExactly("bulbasaur", "ivysaur", "venusaur");
    }

    @Test
    void allowsMissingArtworkCategoryAndDescription() {
        var details = aPokemonDetails().withArtworkUrl(null).withCategory(null).withDescription(null).build();

        assertThat(details.artworkUrl()).isNull();
        assertThat(details.category()).isNull();
        assertThat(details.description()).isNull();
    }

    @Test
    void rejectsNonPositiveId() {
        assertThatIllegalArgumentException().isThrownBy(() -> aPokemonDetails().withId(0).build());
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> aPokemonDetails().withName(" ").build());
    }

    @Test
    void requiresTheEvolutionChain() {
        assertThatNullPointerException()
                .isThrownBy(() -> aPokemonDetails().withEvolutionChain(null).build())
                .withMessageContaining("evolution");
    }

    @Test
    void isNotAffectedByChangesToTheOriginalLists() {
        var types = new ArrayList<>(List.of("grass"));
        var details = aPokemonDetails().withTypes(types).build();

        types.clear();

        assertThat(details.types()).containsExactly("grass");
    }
}
