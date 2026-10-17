package com.lucasamaral.pokedex.web.validation;

/**
 * Accepted shape of a Pokédex number or PokeAPI name in requests. The UI applies the same rule.
 */
public final class PokemonKeyFormat {

    public static final String PATTERN = "[A-Za-z0-9-]{1,50}";
    public static final String MESSAGE = "must be a Pokédex number or a name made of letters, digits and hyphens";

    private PokemonKeyFormat() {
    }
}
