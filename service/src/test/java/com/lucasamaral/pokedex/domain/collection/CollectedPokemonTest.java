package com.lucasamaral.pokedex.domain.collection;

import com.lucasamaral.pokedex.domain.pokemon.Weight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CollectedPokemonTest {

    private static final Instant FIRST_SYNC = Instant.parse("2026-10-17T12:00:00Z");
    private static final Instant SECOND_SYNC = Instant.parse("2026-10-18T09:30:00Z");

    @Test
    void collectingCopiesTheSourceDataAndStartsWithoutProprietaryFields() {
        var bulbasaur = CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC);

        assertThat(bulbasaur.id()).isNotNull();
        assertThat(bulbasaur.pokeapiId()).isEqualTo(1);
        assertThat(bulbasaur.source().name()).isEqualTo("bulbasaur");
        assertThat(bulbasaur.source().types()).containsExactly("grass", "poison");
        assertThat(bulbasaur.source().spriteUrl()).endsWith("/1.png");
        assertThat(bulbasaur.custom()).isEqualTo(CustomFields.empty());
        assertThat(bulbasaur.syncedAt()).isEqualTo(FIRST_SYNC);
    }

    @Test
    void aNewlyCollectedPokemonStartsAtVersionZero() {
        assertThat(CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC).version()).isZero();
    }

    @Test
    void changesKeepTheVersionTheyWereMadeFrom() {
        var stored = new CollectedPokemon(java.util.UUID.randomUUID(),
                SourceData.from(aPokemonDetails().build()), CustomFields.empty(), FIRST_SYNC, 3);

        assertThat(stored.withCustomFields(new CustomFields("Bulby", null, null, Map.of(), Set.of())).version()).isEqualTo(3);
        assertThat(stored.refreshFrom(aPokemonDetails().build(), SECOND_SYNC).version()).isEqualTo(3);
    }

    @Test
    void rejectsANegativeVersion() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CollectedPokemon(java.util.UUID.randomUUID(),
                SourceData.from(aPokemonDetails().build()), CustomFields.empty(), FIRST_SYNC, -1));
    }

    @Test
    void eachCollectedPokemonGetsItsOwnId() {
        var first = CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC);
        var second = CollectedPokemon.collect(aPokemonDetails().withId(4).withName("charmander").build(), FIRST_SYNC);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void reSyncingRefreshesSourceDataAndKeepsProprietaryFields() {
        var custom = new CustomFields("Bulby", "Our first starter", "Kanto", Map.of("pt-BR", "Bulbassauro"), Set.of("starter"));
        var collected = CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC).withCustomFields(custom);

        var refreshed = collected.refreshFrom(aPokemonDetails().withWeight(Weight.ofHectograms(70)).build(), SECOND_SYNC);

        assertThat(refreshed.id()).isEqualTo(collected.id());
        assertThat(refreshed.source().weight()).isEqualTo(Weight.ofHectograms(70));
        assertThat(refreshed.custom()).isEqualTo(custom);
        assertThat(refreshed.syncedAt()).isEqualTo(SECOND_SYNC);
    }

    @Test
    void refusesToRefreshFromAnotherPokemon() {
        var bulbasaur = CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC);
        var charmander = aPokemonDetails().withId(4).withName("charmander").build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> bulbasaur.refreshFrom(charmander, SECOND_SYNC))
                .withMessageContaining("4");
    }

    @Test
    void replacingProprietaryFieldsLeavesSourceDataUntouched() {
        var collected = CollectedPokemon.collect(aPokemonDetails().build(), FIRST_SYNC);

        var edited = collected.withCustomFields(new CustomFields("Bulby", null, null, Map.of(), Set.of()));

        assertThat(edited.source()).isEqualTo(collected.source());
        assertThat(edited.syncedAt()).isEqualTo(FIRST_SYNC);
        assertThat(edited.custom().nickname()).isEqualTo("Bulby");
    }
}
