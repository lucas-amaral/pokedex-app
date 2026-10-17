package com.lucasamaral.pokedex.infrastructure.pokeapi;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Declarative HTTP client for PokeAPI v2. Spring generates the implementation.
 * <p>
 * Responses are cached per resource, so a Pokémon fetched for one page is reused by any other page,
 * and by the detail and sync use cases. {@code sync = true} lets parallel requests for the same
 * resource wait for a single call instead of all hitting PokeAPI. Failures are never cached.
 */
interface PokeApiClient {

    @GetExchange("/pokemon")
    @Cacheable(cacheNames = "pokeapi-pokemon-list", sync = true)
    PokeApiResponses.ResourceList listPokemon(@RequestParam long offset, @RequestParam int limit);

    @GetExchange("/pokemon/{name}")
    @Cacheable(cacheNames = "pokeapi-pokemon", sync = true)
    PokeApiResponses.Pokemon getPokemon(@PathVariable String name);

    @GetExchange("/pokemon-species/{name}")
    @Cacheable(cacheNames = "pokeapi-species", sync = true)
    PokeApiResponses.Species getSpecies(@PathVariable String name);

    @GetExchange("/evolution-chain/{id}")
    @Cacheable(cacheNames = "pokeapi-evolution-chain", sync = true)
    PokeApiResponses.EvolutionChain getEvolutionChain(@PathVariable int id);
}
