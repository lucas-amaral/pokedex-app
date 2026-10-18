package com.lucasamaral.pokedex.domain.collection;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A Pokémon kept in the local collection (US03): data copied from PokeAPI plus our proprietary fields.
 * <p>
 * The two parts have different owners. A re-sync replaces the source data and never touches the proprietary
 * fields; editing (US04) replaces the proprietary fields and never touches the source data.
 *
 * @param syncedAt when the source data was last copied from PokeAPI
 * @param version  how many times the stored copy has changed; an edit must be based on the current version
 */
public record CollectedPokemon(UUID id, SourceData source, CustomFields custom, Instant syncedAt, long version) {

    public CollectedPokemon {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(custom, "custom is required");
        Objects.requireNonNull(syncedAt, "syncedAt is required");
        if (version < 0) {
            throw new IllegalArgumentException("version must be zero or greater, was " + version);
        }
    }

    /** First copy of a Pokémon into the collection, with no proprietary fields yet. */
    public static CollectedPokemon collect(PokemonDetails details, Instant now) {
        return new CollectedPokemon(UUID.randomUUID(), SourceData.from(details), CustomFields.empty(), now, 0);
    }

    /** A new copy of the source data; the proprietary fields stay as they are. */
    public CollectedPokemon refreshFrom(PokemonDetails details, Instant now) {
        if (details.id() != pokeapiId()) {
            throw new IllegalArgumentException(
                    "Cannot refresh Pokémon " + pokeapiId() + " with data of Pokémon " + details.id());
        }
        return new CollectedPokemon(id, SourceData.from(details), custom, now, version);
    }

    public CollectedPokemon withCustomFields(CustomFields newCustom) {
        return new CollectedPokemon(id, source, newCustom, syncedAt, version);
    }

    public int pokeapiId() {
        return source.pokeapiId();
    }
}
