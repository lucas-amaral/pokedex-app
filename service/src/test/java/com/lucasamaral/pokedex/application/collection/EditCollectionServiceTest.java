package com.lucasamaral.pokedex.application.collection;

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

class EditCollectionServiceTest {

    private static final Instant SYNCED = Instant.parse("2026-10-18T10:00:00Z");
    private static final CustomFields EDITED =
            new CustomFields("Bulby", "Our first starter", "Kanto", Map.of("pt-BR", "Bulbassauro"), Set.of("starter"));

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final EditCollectionService edit = new EditCollectionService(repository);
    private final UpdateCollectedPokemonUseCase update = edit;
    private final RemoveCollectedPokemonUseCase remove = edit;

    private CollectedPokemon stored;

    @BeforeEach
    void storeBulbasaur() {
        stored = repository.save(CollectedPokemon.collect(aPokemonDetails().build(), SYNCED));
    }

    @Test
    void replacesTheProprietaryFieldsAndBumpsTheVersion() {
        var updated = update.update(stored.id(), stored.version(), EDITED);

        assertThat(updated.custom()).isEqualTo(EDITED);
        assertThat(updated.version()).isEqualTo(stored.version() + 1);
        assertThat(repository.findById(stored.id())).contains(updated);
    }

    @Test
    void neverTouchesTheSourceDataOrTheSyncTime() {
        var updated = update.update(stored.id(), stored.version(), EDITED);

        assertThat(updated.source()).isEqualTo(stored.source());
        assertThat(updated.syncedAt()).isEqualTo(stored.syncedAt());
    }

    @Test
    void refusesAnEditMadeFromAnOutdatedCopy() {
        update.update(stored.id(), stored.version(), EDITED);

        assertThatExceptionOfType(CollectedPokemonVersionConflictException.class)
                .isThrownBy(() -> update.update(stored.id(), stored.version(), CustomFields.empty()))
                .satisfies(conflict -> {
                    assertThat(conflict.expectedVersion()).isEqualTo(stored.version());
                    assertThat(conflict.currentVersion()).isEqualTo(stored.version() + 1);
                });
        assertThat(repository.findById(stored.id()).orElseThrow().custom()).isEqualTo(EDITED);
    }

    @Test
    void reportsAnEditOfAPokemonThatIsNotInTheCollection() {
        var unknown = UUID.randomUUID();

        assertThatExceptionOfType(CollectedPokemonNotFoundException.class)
                .isThrownBy(() -> update.update(unknown, 0, EDITED));
    }

    @Test
    void removesAPokemonFromTheCollection() {
        remove.remove(stored.id());

        assertThat(repository.findById(stored.id())).isEmpty();
    }

    @Test
    void reportsTheRemovalOfAPokemonThatIsNotInTheCollection() {
        var unknown = UUID.randomUUID();

        assertThatExceptionOfType(CollectedPokemonNotFoundException.class).isThrownBy(() -> remove.remove(unknown));
    }
}
