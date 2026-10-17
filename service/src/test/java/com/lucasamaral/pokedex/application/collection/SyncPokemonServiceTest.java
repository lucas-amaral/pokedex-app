package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.Weight;
import com.lucasamaral.pokedex.support.InMemoryCollectionRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SyncPokemonServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    private final FakeDetailsCatalog catalog = new FakeDetailsCatalog();
    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final SyncPokemonUseCase sync = new SyncPokemonService(catalog, repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void addsAPokemonThatIsNotInTheCollectionYet() {
        catalog.add(aPokemonDetails().build());

        var result = sync.sync("bulbasaur");

        assertThat(result.created()).isTrue();
        assertThat(result.pokemon().pokeapiId()).isEqualTo(1);
        assertThat(result.pokemon().syncedAt()).isEqualTo(NOW);
        assertThat(repository.all()).containsExactly(result.pokemon());
    }

    @Test
    void refreshesAPokemonAlreadyInTheCollectionAndKeepsItsProprietaryFields() {
        var custom = new CustomFields("Bulby", null, "Kanto", Map.of(), Set.of("starter"));
        var existing = repository.save(
                CollectedPokemon.collect(aPokemonDetails().build(), Instant.parse("2026-10-01T00:00:00Z")).withCustomFields(custom));
        catalog.add(aPokemonDetails().withWeight(Weight.ofHectograms(70)).build());

        var result = sync.sync("bulbasaur");

        assertThat(result.created()).isFalse();
        assertThat(result.pokemon().id()).isEqualTo(existing.id());
        assertThat(result.pokemon().source().weight()).isEqualTo(Weight.ofHectograms(70));
        assertThat(result.pokemon().custom()).isEqualTo(custom);
        assertThat(result.pokemon().syncedAt()).isEqualTo(NOW);
        assertThat(repository.all()).hasSize(1);
    }

    @Test
    void recordsTheSyncTimeAtTheDatabasePrecision() {
        catalog.add(aPokemonDetails().build());
        var nanosClock = Clock.fixed(Instant.parse("2026-10-18T10:00:00.123456789Z"), ZoneOffset.UTC);

        var result = new SyncPokemonService(catalog, repository, nanosClock).sync("bulbasaur");

        assertThat(result.pokemon().syncedAt()).isEqualTo(Instant.parse("2026-10-18T10:00:00.123456Z"));
    }

    @Test
    void findsTheSamePokemonWhetherAskedByNameOrNumber() {
        catalog.add(aPokemonDetails().build());

        sync.sync("bulbasaur");
        var second = sync.sync("1");

        assertThat(second.created()).isFalse();
        assertThat(repository.all()).hasSize(1);
    }

    @Test
    void asksTheCatalogWithANormalizedKey() {
        catalog.add(aPokemonDetails().build());

        sync.sync("  Bulbasaur ");

        assertThat(catalog.requestedKeys).containsExactly("bulbasaur");
    }

    @Test
    void reportsAPokemonPokeApiDoesNotKnowAndStoresNothing() {
        assertThatExceptionOfType(PokemonNotFoundException.class).isThrownBy(() -> sync.sync("missingno"));

        assertThat(repository.all()).isEmpty();
    }

    @Test
    void storesNothingWhenPokeApiIsUnavailable() {
        catalog.failWith(new CatalogUnavailableException("PokeAPI down", new IOException("boom")));

        assertThatExceptionOfType(CatalogUnavailableException.class).isThrownBy(() -> sync.sync("bulbasaur"));

        assertThat(repository.all()).isEmpty();
    }

    @Test
    void refreshesWhenAnotherRequestAddedTheSamePokemonFirst() {
        catalog.add(aPokemonDetails().build());
        var racing = new RacingRepository(repository);
        var racingSync = new SyncPokemonService(catalog, racing, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = racingSync.sync("bulbasaur");

        assertThat(result.created()).isFalse();
        assertThat(result.pokemon().id()).isEqualTo(racing.winner.id());
        assertThat(repository.all()).hasSize(1);
    }

    /**
     * Simulates a concurrent request: the first lookup finds nothing, but by the time this request saves,
     * another one has already stored the same Pokémon, so the insert is refused as a duplicate.
     */
    private static final class RacingRepository implements com.lucasamaral.pokedex.application.collection.CollectionRepository {

        private final InMemoryCollectionRepository delegate;
        private CollectedPokemon winner;

        private RacingRepository(InMemoryCollectionRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<CollectedPokemon> findById(java.util.UUID id) {
            return delegate.findById(id);
        }

        @Override
        public Optional<CollectedPokemon> findByPokeapiId(int pokeapiId) {
            return winner == null ? Optional.empty() : delegate.findByPokeapiId(pokeapiId);
        }

        @Override
        public CollectedPokemon save(CollectedPokemon pokemon) {
            if (winner == null) {
                winner = delegate.save(CollectedPokemon.collect(aPokemonDetails().build(), NOW.minusSeconds(1)));
                throw new DuplicateCollectedPokemonException(pokemon.pokeapiId(), new IllegalStateException("unique key"));
            }
            return delegate.save(pokemon);
        }

        @Override
        public com.lucasamaral.pokedex.application.pagination.Page<CollectedPokemon> findPage(
                CollectionFilter filter, com.lucasamaral.pokedex.application.pagination.PageQuery query) {
            return delegate.findPage(filter, query);
        }
    }

    /**
     * Fake detail catalog answering by name or Pokédex number, and recording the keys it was asked for.
     */
    private static final class FakeDetailsCatalog implements PokemonDetailsCatalog {

        private final Map<String, PokemonDetails> byKey = new HashMap<>();
        private final List<String> requestedKeys = new ArrayList<>();
        private RuntimeException failure;

        void add(PokemonDetails details) {
            byKey.put(details.name(), details);
            byKey.put(String.valueOf(details.id()), details);
        }

        void failWith(RuntimeException exception) {
            failure = exception;
        }

        @Override
        public Optional<PokemonDetails> findDetails(String idOrName) {
            requestedKeys.add(idOrName);
            if (failure != null) {
                throw failure;
            }
            return Optional.ofNullable(byKey.get(idOrName));
        }
    }
}
