package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import com.lucasamaral.pokedex.support.InMemoryCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BrowseCollectionServiceTest {

    private static final Instant SYNCED = Instant.parse("2026-10-18T10:00:00Z");

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final BrowseCollectionUseCase collection = new BrowseCollectionService(repository);

    private CollectedPokemon bulbasaur;

    @BeforeEach
    void fillTheCollection() {
        bulbasaur = repository.save(CollectedPokemon.collect(aPokemonDetails().build(), SYNCED)
                .withCustomFields(new CustomFields("Bulby", null, null, Map.of(), Set.of("starter"))));
        repository.save(CollectedPokemon.collect(aPokemonDetails().withId(4).withName("charmander").build(), SYNCED));
    }

    @Test
    void listsTheCollectionPageByPage() {
        var page = collection.list(CollectionFilter.none(), new PageQuery(0, 1));

        assertThat(page.content()).extracting(CollectedPokemon::pokeapiId).containsExactly(1);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void passesANormalizedFilterToTheRepository() {
        collection.list(new CollectionFilter("  BULB ", " Starter "), PageQuery.first(20));

        assertThat(repository.lastFilter()).isEqualTo(new CollectionFilter("bulb", "starter"));
    }

    @Test
    void passesThePokeApiNumberFilterThrough() {
        collection.list(new CollectionFilter(null, null, 4), PageQuery.first(1));

        assertThat(repository.lastFilter().pokeapiId()).isEqualTo(4);
    }

    @Test
    void treatsBlankFilterValuesAsNoFilter() {
        assertThat(new CollectionFilter("  ", "")).isEqualTo(CollectionFilter.none());
    }

    @Test
    void getsOnePokemonOfTheCollection() {
        assertThat(collection.get(bulbasaur.id())).isEqualTo(bulbasaur);
    }

    @Test
    void reportsAPokemonThatIsNotInTheCollection() {
        var unknown = UUID.randomUUID();

        assertThatExceptionOfType(CollectedPokemonNotFoundException.class)
                .isThrownBy(() -> collection.get(unknown))
                .withMessageContaining(unknown.toString());
    }
}
