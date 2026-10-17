package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.util.Locale;
import java.util.Objects;

public class GetPokemonDetailsService implements GetPokemonDetailsUseCase {

    private final PokemonDetailsCatalog catalog;

    public GetPokemonDetailsService(PokemonDetailsCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public PokemonDetails getDetails(String idOrName) {
        var key = normalize(idOrName);
        return catalog.findDetails(key).orElseThrow(() -> new PokemonNotFoundException(key));
    }

    private static String normalize(String idOrName) {
        Objects.requireNonNull(idOrName, "idOrName is required");
        var key = idOrName.strip().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("idOrName must not be blank");
        }
        return key;
    }
}
