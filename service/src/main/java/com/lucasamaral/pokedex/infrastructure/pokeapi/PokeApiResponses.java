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
            int height,
            int weight,
            NamedResource species,
            Sprites sprites,
            List<PokemonAbility> abilities,
            List<PokemonType> types,
            List<PokemonStat> stats,
            @JsonProperty("is_default") Boolean isDefault) {
    }

    record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
    }

    record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    record PokemonAbility(NamedResource ability, @JsonProperty("is_hidden") boolean hidden, int slot) {
    }

    record PokemonType(int slot, NamedResource type) {
    }

    record PokemonStat(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
    }

    record Species(
            int id,
            String name,
            List<Genus> genera,
            @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
            @JsonProperty("evolution_chain") NamedResource evolutionChain) {
    }

    record Genus(String genus, NamedResource language) {
    }

    record FlavorText(@JsonProperty("flavor_text") String text, NamedResource language) {
    }

    record EvolutionChain(int id, ChainLink chain) {
    }

    record ChainLink(
            NamedResource species,
            @JsonProperty("evolution_details") List<EvolutionDetail> evolutionDetails,
            @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
    }

    /** One way to evolve; PokeAPI lists one per game and marks the current one with {@code is_default}. */
    record EvolutionDetail(
            NamedResource trigger,
            @JsonProperty("min_level") Integer minLevel,
            NamedResource item,
            @JsonProperty("held_item") NamedResource heldItem,
            @JsonProperty("min_happiness") Integer minHappiness,
            @JsonProperty("time_of_day") String timeOfDay,
            @JsonProperty("known_move") NamedResource knownMove,
            @JsonProperty("known_move_type") NamedResource knownMoveType,
            @JsonProperty("is_default") Boolean isDefault) {
    }
}
