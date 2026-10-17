package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static com.lucasamaral.pokedex.support.PokemonSummaryBuilder.aPokemonSummary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class BrowseCatalogServiceTest {

    private final List<PokemonSummary> catalog = List.of(
            aPokemonSummary().withId(1).withName("bulbasaur").build(),
            aPokemonSummary().withId(2).withName("ivysaur").build(),
            aPokemonSummary().withId(3).withName("venusaur").build());

    private final BrowseCatalogUseCase browseCatalog = new BrowseCatalogService(new InMemoryPokemonCatalog(catalog));

    @Test
    void returnsTheRequestedPage() {
        var page = browseCatalog.browse(new PageQuery(1, 2));

        assertThat(page.content()).extracting(PokemonSummary::name).containsExactly("venusaur");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void returnsAnEmptyPageBeyondTheEnd() {
        var page = browseCatalog.browse(new PageQuery(5, 2));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void requiresAQuery() {
        assertThatNullPointerException().isThrownBy(() -> browseCatalog.browse(null));
    }

    /**
     * Hand-written fake of the output port: keeps the test about behaviour, not about mock interactions.
     */
    private record InMemoryPokemonCatalog(List<PokemonSummary> pokemon) implements PokemonCatalog {

        @Override
        public Page<PokemonSummary> findSummaries(PageQuery query) {
            Objects.requireNonNull(query);
            var content = pokemon.stream().skip(query.offset()).limit(query.size()).toList();
            return new Page<>(content, query, pokemon.size());
        }
    }
}
