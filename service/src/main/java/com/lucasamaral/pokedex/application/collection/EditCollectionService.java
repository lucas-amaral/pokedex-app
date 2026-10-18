package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;

import java.util.Objects;
import java.util.UUID;

public class EditCollectionService implements UpdateCollectedPokemonUseCase, RemoveCollectedPokemonUseCase {

    private final CollectionRepository repository;

    public EditCollectionService(CollectionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public CollectedPokemon update(UUID id, long expectedVersion, CustomFields fields) {
        Objects.requireNonNull(fields, "fields are required");
        var stored = repository.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new CollectedPokemonNotFoundException(id));
        if (stored.version() != expectedVersion) {
            throw new CollectedPokemonVersionConflictException(id, expectedVersion, stored.version());
        }
        return repository.save(stored.withCustomFields(fields));
    }

    @Override
    public void remove(UUID id) {
        if (!repository.deleteById(Objects.requireNonNull(id, "id is required"))) {
            throw new CollectedPokemonNotFoundException(id);
        }
    }
}
