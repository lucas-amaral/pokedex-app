package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Output adapter: the details catalog read from a JSON snapshot of PokeAPI instead of from PokeAPI itself.
 * <p>
 * Strategy: the sync use case does not change; seeding hands it this catalog, the API hands it the live one.
 */
public final class SnapshotPokemonCatalog implements PokemonDetailsCatalog {

    private final Map<String, PokemonDetails> byId;
    private final Map<String, PokemonDetails> byName;

    private SnapshotPokemonCatalog(List<PokemonDetails> pokemon) {
        byId = new LinkedHashMap<>();
        byName = new LinkedHashMap<>();
        pokemon.forEach(details -> {
            byId.put(String.valueOf(details.id()), details);
            byName.put(details.name(), details);
        });
    }

    /** @throws IllegalStateException when the snapshot is missing or malformed */
    public static SnapshotPokemonCatalog load(Resource snapshot, JsonMapper json) {
        try (var input = snapshot.getInputStream()) {
            var file = json.readValue(input, SnapshotFile.class);
            return new SnapshotPokemonCatalog(file.pokemon().stream().map(SnapshotFile.Entry::toDetails).toList());
        } catch (IOException | RuntimeException unreadable) {
            throw new IllegalStateException("Cannot read the Pokémon snapshot " + snapshot.getDescription(), unreadable);
        }
    }

    /** The ids of every Pokémon in the snapshot, in the order the file lists them. */
    public List<String> keys() {
        return List.copyOf(byId.keySet());
    }

    @Override
    public Optional<PokemonDetails> findDetails(String idOrName) {
        return Optional.ofNullable(byId.getOrDefault(idOrName, byName.get(idOrName)));
    }
}
