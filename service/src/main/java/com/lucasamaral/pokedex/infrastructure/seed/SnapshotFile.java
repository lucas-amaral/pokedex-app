package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.time.Instant;
import java.util.List;

/**
 * The JSON layout of a PokeAPI snapshot: the data the collection copies from PokeAPI, for a list of Pokémon.
 *
 * @param source     where the data was read from
 * @param exportedAt when it was read
 */
record SnapshotFile(String source, Instant exportedAt, List<Entry> pokemon) {

    SnapshotFile {
        pokemon = List.copyOf(pokemon);
    }

    record Entry(
            int id,
            String name,
            String speciesName,
            String category,
            String description,
            String spriteUrl,
            String artworkUrl,
            int heightDecimeters,
            int weightHectograms,
            List<String> types,
            List<Ability> abilities,
            List<BaseStat> stats) {

        static Entry from(PokemonDetails details) {
            return new Entry(details.id(), details.name(), details.speciesName(), details.category(), details.description(),
                    details.spriteUrl(), details.artworkUrl(), details.height().decimeters(), details.weight().hectograms(),
                    details.types(), details.abilities(), details.stats());
        }

        /**
         * The evolution chain is not in the snapshot (the collection does not store it; the detail page reads it live),
         * so the Pokémon stands alone in its chain.
         */
        PokemonDetails toDetails() {
            var aloneInItsChain = new EvolutionNode(id, speciesName, spriteUrl, null, List.of());
            return new PokemonDetails(id, name, speciesName, spriteUrl, artworkUrl, category, description,
                    Height.ofDecimeters(heightDecimeters), Weight.ofHectograms(weightHectograms), types, abilities, stats,
                    aloneInItsChain);
        }
    }
}
