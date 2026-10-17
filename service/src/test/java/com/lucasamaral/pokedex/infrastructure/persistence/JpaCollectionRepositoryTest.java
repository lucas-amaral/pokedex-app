package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.DuplicateCollectedPokemonException;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import com.lucasamaral.pokedex.domain.pokemon.Weight;
import com.lucasamaral.pokedex.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestConfiguration.class, JpaCollectionRepository.class})
class JpaCollectionRepositoryTest {

    private static final Instant SYNCED = Instant.parse("2026-10-18T10:00:00Z");

    @Autowired
    private CollectionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PokemonJpaRepository jpa;

    @Test
    void storesAndReadsBackEverySourceAndProprietaryField() {
        var custom = new CustomFields("Bulby", "Our first starter", "Kanto",
                Map.of("pt-BR", "Bulbassauro", "ja", "フシギダネ"), Set.of("starter", "grass-team"));
        var bulbasaur = CollectedPokemon.collect(aPokemonDetails().build(), SYNCED).withCustomFields(custom);

        repository.save(bulbasaur);
        flushAndClear();

        assertThat(repository.findById(bulbasaur.id())).contains(bulbasaur);
    }

    @Test
    void findsAPokemonByItsPokeApiNumber() {
        var bulbasaur = repository.save(CollectedPokemon.collect(aPokemonDetails().build(), SYNCED));
        flushAndClear();

        assertThat(repository.findByPokeapiId(1)).contains(bulbasaur);
        assertThat(repository.findByPokeapiId(25)).isEmpty();
    }

    @Test
    void replacesTheStoredCopyWhenSavedAgain() {
        var stored = repository.save(CollectedPokemon.collect(aPokemonDetails().build(), SYNCED));
        flushAndClear();

        var refreshed = stored
                .refreshFrom(aPokemonDetails().withWeight(Weight.ofHectograms(70)).build(), SYNCED.plusSeconds(60))
                .withCustomFields(new CustomFields("Bulby", null, null, Map.of(), Set.of("starter")));
        repository.save(refreshed);
        flushAndClear();

        assertThat(repository.findById(stored.id())).contains(refreshed);
        assertThat(repository.findPage(CollectionFilter.none(), PageQuery.first(20)).totalElements()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesASecondCopyOfTheSamePokemon() {
        repository.save(CollectedPokemon.collect(aPokemonDetails().build(), SYNCED));
        var secondCopy = CollectedPokemon.collect(aPokemonDetails().build(), SYNCED);

        try {
            assertThatExceptionOfType(DuplicateCollectedPokemonException.class)
                    .isThrownBy(() -> repository.save(secondCopy));
        } finally {
            jpa.deleteAll();
        }
    }

    @Test
    void listsTheCollectionInPokedexOrderPageByPage() {
        saveAll("charmander", 4, null, Set.of());
        saveAll("bulbasaur", 1, null, Set.of());
        saveAll("squirtle", 7, null, Set.of());

        var firstPage = repository.findPage(CollectionFilter.none(), new PageQuery(0, 2));
        var secondPage = repository.findPage(CollectionFilter.none(), new PageQuery(1, 2));

        assertThat(firstPage.content()).extracting(CollectedPokemon::pokeapiId).containsExactly(1, 4);
        assertThat(secondPage.content()).extracting(CollectedPokemon::pokeapiId).containsExactly(7);
        assertThat(firstPage.totalElements()).isEqualTo(3);
    }

    @Test
    void filtersByPartOfTheNameOrNickname() {
        saveAll("bulbasaur", 1, null, Set.of());
        saveAll("charmander", 4, "Flamey", Set.of());
        saveAll("squirtle", 7, null, Set.of());

        assertThat(repository.findPage(new CollectionFilter("saur", null), PageQuery.first(20)).content())
                .extracting(CollectedPokemon::pokeapiId).containsExactly(1);
        assertThat(repository.findPage(new CollectionFilter("flame", null), PageQuery.first(20)).content())
                .extracting(CollectedPokemon::pokeapiId).containsExactly(4);
    }

    @Test
    void filtersByTag() {
        saveAll("bulbasaur", 1, null, Set.of("starter", "kanto"));
        saveAll("pidgey", 16, null, Set.of("kanto"));

        var page = repository.findPage(new CollectionFilter(null, "starter"), PageQuery.first(20));

        assertThat(page.content()).extracting(CollectedPokemon::pokeapiId).containsExactly(1);
        assertThat(page.content().getFirst().custom().tags()).containsExactly("kanto", "starter");
    }

    @Test
    void combinesNameAndTagFilters() {
        saveAll("bulbasaur", 1, null, Set.of("starter"));
        saveAll("charmander", 4, null, Set.of("starter"));

        var page = repository.findPage(new CollectionFilter("char", "starter"), PageQuery.first(20));

        assertThat(page.content()).extracting(CollectedPokemon::pokeapiId).containsExactly(4);
    }

    private void saveAll(String name, int id, String nickname, Set<String> tags) {
        repository.save(CollectedPokemon.collect(aPokemonDetails().withId(id).withName(name).build(), SYNCED)
                .withCustomFields(new CustomFields(nickname, null, null, Map.of(), tags)));
        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
