package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.catalog.PokemonKey;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class SyncPokemonService implements SyncPokemonUseCase {

    private final PokemonDetailsCatalog catalog;
    private final CollectionRepository repository;
    private final Clock clock;

    public SyncPokemonService(PokemonDetailsCatalog catalog, CollectionRepository repository, Clock clock) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public SyncResult sync(String idOrName) {
        var key = PokemonKey.normalize(idOrName);
        var details = catalog.findDetails(key).orElseThrow(() -> new PokemonNotFoundException(key));
        // PostgreSQL keeps microseconds; truncating here makes the response match what a later read returns.
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);

        var existing = repository.findByPokeapiId(details.id());
        if (existing.isPresent()) {
            return refresh(existing.get(), details, now);
        }
        try {
            return new SyncResult(repository.save(CollectedPokemon.collect(details, now)), true);
        } catch (DuplicateCollectedPokemonException raceLost) {
            // A concurrent request stored this Pokémon between our lookup and our insert: refresh its copy instead.
            var stored = repository.findByPokeapiId(details.id()).orElseThrow(() -> raceLost);
            return refresh(stored, details, now);
        }
    }

    private SyncResult refresh(CollectedPokemon stored, PokemonDetails details, Instant now) {
        return new SyncResult(repository.save(stored.refreshFrom(details, now)), false);
    }
}
