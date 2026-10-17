package com.lucasamaral.pokedex.infrastructure.pokeapi;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Declarative HTTP client for PokeAPI v2. Spring generates the implementation.
 */
interface PokeApiClient {

    @GetExchange("/pokemon")
    PokeApiResponses.ResourceList listPokemon(@RequestParam long offset, @RequestParam int limit);

    @GetExchange("/pokemon/{name}")
    PokeApiResponses.Pokemon getPokemon(@PathVariable String name);

    @GetExchange("/pokemon-species/{name}")
    PokeApiResponses.Species getSpecies(@PathVariable String name);
}
