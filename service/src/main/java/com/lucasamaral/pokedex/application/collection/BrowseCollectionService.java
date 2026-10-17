package com.lucasamaral.pokedex.application.collection;

import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.util.Objects;
import java.util.UUID;

public class BrowseCollectionService implements BrowseCollectionUseCase {

    private final CollectionRepository repository;

    public BrowseCollectionService(CollectionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public Page<CollectedPokemon> list(CollectionFilter filter, PageQuery query) {
        return repository.findPage(Objects.requireNonNull(filter, "filter is required"),
                Objects.requireNonNull(query, "query is required"));
    }

    @Override
    public CollectedPokemon get(UUID id) {
        Objects.requireNonNull(id, "id is required");
        return repository.findById(id).orElseThrow(() -> new CollectedPokemonNotFoundException(id));
    }
}
