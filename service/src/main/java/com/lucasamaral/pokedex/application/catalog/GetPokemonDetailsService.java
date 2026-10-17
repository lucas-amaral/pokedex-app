package com.lucasamaral.pokedex.application.catalog;

import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;

import java.util.Objects;

public class GetPokemonDetailsService implements GetPokemonDetailsUseCase {

    private final PokemonDetailsCatalog catalog;

    public GetPokemonDetailsService(PokemonDetailsCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public PokemonDetails getDetails(String idOrName) {
        var key = PokemonKey.normalize(idOrName);
        return catalog.findDetails(key).orElseThrow(() -> new PokemonNotFoundException(key));
    }
}
