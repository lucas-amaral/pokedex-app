package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class GetPokemonDetailsServiceTest {

    private final PokemonDetails bulbasaur = aPokemonDetails().build();
    private final InMemoryDetailsCatalog catalog = new InMemoryDetailsCatalog(Map.of("bulbasaur", bulbasaur, "1", bulbasaur));
    private final GetPokemonDetailsUseCase getDetails = new GetPokemonDetailsService(catalog);

    @ParameterizedTest
    @ValueSource(strings = {"bulbasaur", "1"})
    void findsAPokemonByNameOrNumber(String idOrName) {
        assertThat(getDetails.getDetails(idOrName)).isEqualTo(bulbasaur);
    }

    @Test
    void ignoresCaseAndSurroundingSpaces() {
        getDetails.getDetails("  Bulbasaur ");

        assertThat(catalog.requestedKeys).containsExactly("bulbasaur");
    }

    @Test
    void reportsAPokemonThatDoesNotExist() {
        assertThatExceptionOfType(PokemonNotFoundException.class)
                .isThrownBy(() -> getDetails.getDetails("missingno"))
                .withMessageContaining("missingno");
    }

    @Test
    void rejectsABlankKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> getDetails.getDetails("  "));
    }

    @Test
    void requiresAKey() {
        assertThatNullPointerException().isThrownBy(() -> getDetails.getDetails(null));
    }

    /**
     * Hand-written fake of the output port that also records what it was asked for.
     */
    private static final class InMemoryDetailsCatalog implements PokemonDetailsCatalog {

        private final Map<String, PokemonDetails> pokemon;
        private final List<String> requestedKeys = new ArrayList<>();

        private InMemoryDetailsCatalog(Map<String, PokemonDetails> pokemon) {
            this.pokemon = pokemon;
        }

        @Override
        public Optional<PokemonDetails> findDetails(String idOrName) {
            requestedKeys.add(idOrName);
            return Optional.ofNullable(pokemon.get(idOrName));
        }
    }
}
