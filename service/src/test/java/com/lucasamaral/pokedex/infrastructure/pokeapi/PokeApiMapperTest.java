package com.lucasamaral.pokedex.infrastructure.pokeapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiMapperTest {

    private static final PokeApiResponses.NamedResource SPECIES = new PokeApiResponses.NamedResource("bulbasaur", null);

    @Test
    void leavesSpriteEmptyWhenPokeApiSendsNoSprites() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 69, SPECIES, null, List.of());

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).spriteUrl()).isNull();
    }

    @Test
    void leavesCategoryEmptyWhenPokeApiSendsNoGenera() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 69, SPECIES, new PokeApiResponses.Sprites(null), List.of());

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(null)).category()).isNull();
    }

    @Test
    void returnsNoAbilitiesWhenPokeApiSendsNone() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 69, SPECIES, new PokeApiResponses.Sprites(null), null);

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).abilities()).isEmpty();
    }

    private static PokeApiResponses.Species speciesWithGenera(List<PokeApiResponses.Genus> genera) {
        return new PokeApiResponses.Species(1, "bulbasaur", genera);
    }
}
