package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.PokemonCatalog;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Output adapter: serves the catalog from PokeAPI.
 * <p>
 * PokeAPI's list endpoint returns only names, so each entry needs two more calls (pokemon and species).
 * Entries are fetched in parallel on the given executor (virtual threads in production).
 */
final class PokeApiCatalogAdapter implements PokemonCatalog {

    private final PokeApiClient client;
    private final Executor executor;

    PokeApiCatalogAdapter(PokeApiClient client, Executor executor) {
        this.client = Objects.requireNonNull(client, "client is required");
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public Page<PokemonSummary> findSummaries(PageQuery query) {
        var list = call(() -> client.listPokemon(query.offset(), query.size()));
        var futures = list.results().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> fetchSummary(entry.name()), executor))
                .toList();
        return new Page<>(joinAll(futures), query, list.count());
    }

    private PokemonSummary fetchSummary(String name) {
        var pokemon = call(() -> client.getPokemon(name));
        var species = call(() -> client.getSpecies(pokemon.species().name()));
        return PokeApiMapper.toSummary(pokemon, species);
    }

    private static List<PokemonSummary> joinAll(List<CompletableFuture<PokemonSummary>> futures) {
        try {
            return futures.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException e) {
            if (e.getCause() instanceof CatalogUnavailableException unavailable) {
                throw unavailable;
            }
            throw e;
        }
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("PokeAPI request failed: " + e.getMessage(), e);
        }
    }
}
