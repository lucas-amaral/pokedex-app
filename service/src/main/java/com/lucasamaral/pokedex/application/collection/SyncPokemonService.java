package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.catalog.PokemonKey;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.time.Clock;
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
        var now = clock.instant();

        return repository.findByPokeapiId(details.id())
                .map(existing -> new SyncResult(repository.save(existing.refreshFrom(details, now)), false))
                .orElseGet(() -> new SyncResult(repository.save(CollectedPokemon.collect(details, now)), true));
    }
}
