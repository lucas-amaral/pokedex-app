package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SnapshotPokemonCatalogTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final SnapshotPokemonCatalog catalog =
            SnapshotPokemonCatalog.load(new ClassPathResource("seed/test-snapshot.json"), json);

    @Test
    void listsTheKeysOfEveryPokemonInPokedexOrder() {
        assertThat(catalog.keys()).containsExactly("1", "152");
    }

    @Test
    void findsAPokemonByIdOrName() {
        var bulbasaur = catalog.findDetails("1").orElseThrow();

        assertThat(catalog.findDetails("bulbasaur")).contains(bulbasaur);
        assertThat(bulbasaur.name()).isEqualTo("bulbasaur");
        assertThat(bulbasaur.speciesName()).isEqualTo("bulbasaur");
        assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.description()).startsWith("A strange seed");
        assertThat(bulbasaur.spriteUrl()).endsWith("/pokemon/1.png");
        assertThat(bulbasaur.artworkUrl()).endsWith("/official-artwork/1.png");
        assertThat(bulbasaur.height()).isEqualTo(Height.ofDecimeters(7));
        assertThat(bulbasaur.weight()).isEqualTo(Weight.ofHectograms(69));
        assertThat(bulbasaur.types()).containsExactly("grass", "poison");
        assertThat(bulbasaur.abilities()).containsExactly(new Ability("overgrow", false), new Ability("chlorophyll", true));
        assertThat(bulbasaur.stats()).containsExactly(new BaseStat("hp", 45), new BaseStat("attack", 49));
    }

    @Test
    void keepsFieldsPokeApiDoesNotHaveAsAbsent() {
        var chikorita = catalog.findDetails("152").orElseThrow();

        assertThat(chikorita.description()).isNull();
        assertThat(chikorita.spriteUrl()).isNull();
        assertThat(chikorita.artworkUrl()).isNull();
    }

    @Test
    void findsNothingForAPokemonOutsideTheSnapshot() {
        assertThat(catalog.findDetails("25")).isEmpty();
        assertThat(catalog.findDetails("pikachu")).isEmpty();
    }

    @Test
    void refusesASnapshotThatCannotBeRead() {
        assertThatIllegalStateException()
                .isThrownBy(() -> SnapshotPokemonCatalog.load(new ClassPathResource("seed/missing.json"), json))
                .withMessageContaining("seed/missing.json");
    }

    @Test
    void shipsEveryPokemonOfGenerationsOneAndTwo() {
        var shipped = SnapshotPokemonCatalog.load(new ClassPathResource(SeedProperties.DEFAULT_SNAPSHOT), json);

        assertThat(shipped.keys()).hasSize(251).startsWith("1").endsWith("251");
        assertThat(shipped.findDetails("pikachu")).hasValueSatisfying(pikachu -> {
            assertThat(pikachu.id()).isEqualTo(25);
            assertThat(pikachu.types()).isEqualTo(List.of("electric"));
            assertThat(pikachu.category()).isEqualTo("Mouse Pokémon");
        });
    }
}
