package com.lucasamaral.pokedex.support;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hand-written fake of the collection repository, keeping Pokémon in a map. It also records the last filter
 * it was asked for, so tests can check what the use case passed down.
 */
public final class InMemoryCollectionRepository implements CollectionRepository {

    private final Map<UUID, CollectedPokemon> pokemon = new LinkedHashMap<>();
    private CollectionFilter lastFilter;

    @Override
    public Optional<CollectedPokemon> findById(UUID id) {
        return Optional.ofNullable(pokemon.get(id));
    }

    @Override
    public Optional<CollectedPokemon> findByPokeapiId(int pokeapiId) {
        return pokemon.values().stream().filter(candidate -> candidate.pokeapiId() == pokeapiId).findFirst();
    }

    @Override
    public CollectedPokemon save(CollectedPokemon collected) {
        pokemon.put(collected.id(), collected);
        return collected;
    }

    @Override
    public Page<CollectedPokemon> findPage(CollectionFilter filter, PageQuery query) {
        lastFilter = filter;
        var matching = pokemon.values().stream()
                .filter(candidate -> filter.name() == null
                        || candidate.source().name().contains(filter.name())
                        || (candidate.custom().nickname() != null
                            && candidate.custom().nickname().toLowerCase(Locale.ROOT).contains(filter.name()))
                        || candidate.custom().localizedNames().values().stream()
                            .anyMatch(localized -> localized.toLowerCase(Locale.ROOT).contains(filter.name())))
                .filter(candidate -> filter.tag() == null || candidate.custom().tags().contains(filter.tag()))
                .filter(candidate -> filter.pokeapiId() == null || candidate.pokeapiId() == filter.pokeapiId())
                .sorted(Comparator.comparingInt(CollectedPokemon::pokeapiId))
                .toList();
        var content = matching.stream().skip(query.offset()).limit(query.size()).toList();
        return new Page<>(content, query, matching.size());
    }

    public List<CollectedPokemon> all() {
        return List.copyOf(pokemon.values());
    }

    public CollectionFilter lastFilter() {
        return lastFilter;
    }
}
