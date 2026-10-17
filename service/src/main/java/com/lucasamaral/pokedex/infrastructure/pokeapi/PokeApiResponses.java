package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The parts of PokeAPI's JSON this service reads. Unknown fields are ignored.
 */
final class PokeApiResponses {

    private PokeApiResponses() {
    }

    record NamedResource(String name, String url) {
    }

    record ResourceList(long count, List<NamedResource> results) {
    }

    record Pokemon(
            int id,
            String name,
            int weight,
            NamedResource species,
            Sprites sprites,
            List<PokemonAbility> abilities) {
    }

    record Sprites(@JsonProperty("front_default") String frontDefault) {
    }

    record PokemonAbility(NamedResource ability, @JsonProperty("is_hidden") boolean hidden, int slot) {
    }

    record Species(int id, String name, List<Genus> genera) {
    }

    record Genus(String genus, NamedResource language) {
    }
}
