package com.lucasamaral.pokedex.application.collection;

/**
 * Input port for US03: copy a Pokémon from PokeAPI into the collection, or refresh the copy already there.
 */
public interface SyncPokemonUseCase {

    /**
     * @param idOrName Pokédex number or name, case-insensitive
     * @throws com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException when PokeAPI does not know it
     */
    SyncResult sync(String idOrName);
}
