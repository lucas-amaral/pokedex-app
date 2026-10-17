package com.lucasamaral.pokedex.infrastructure.pokeapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiMapperTest {

    private static final PokeApiResponses.NamedResource SPECIES = new PokeApiResponses.NamedResource("bulbasaur", null);

    @Test
    void leavesSpriteEmptyWhenPokeApiSendsNoSprites() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of());

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).spriteUrl()).isNull();
    }

    @Test
    void leavesCategoryEmptyWhenPokeApiSendsNoGenera() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, new PokeApiResponses.Sprites(null, null), List.of(), List.of(), List.of());

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(null)).category()).isNull();
    }

    @Test
    void returnsNoAbilitiesWhenPokeApiSendsNone() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, new PokeApiResponses.Sprites(null, null), null, List.of(), List.of());

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).abilities()).isEmpty();
    }

    private static PokeApiResponses.Species speciesWithGenera(List<PokeApiResponses.Genus> genera) {
        return new PokeApiResponses.Species(1, "bulbasaur", genera, List.of(), null);
    }

    @Test
    void leavesArtworkEmptyWhenPokeApiSendsNone() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES,
                new PokeApiResponses.Sprites(null, null), List.of(), List.of(), List.of());

        assertThat(PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(List.of())).artworkUrl()).isNull();
    }

    @Test
    void leavesTheEvolutionConditionEmptyWhenPokeApiSendsNoDetails() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of());

        var chain = PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(List.of())).evolutionChain();

        assertThat(chain.evolvesTo().getFirst().condition()).isNull();
    }

    @Test
    void readsTheIdAtTheEndOfAPokeApiLink() {
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/evolution-chain/67/")).isEqualTo(67);
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/pokemon-species/133")).isEqualTo(133);
    }

    private static PokeApiResponses.EvolutionChain chainOf(List<PokeApiResponses.EvolutionDetail> ivysaurDetails) {
        var ivysaur = new PokeApiResponses.ChainLink(
                new PokeApiResponses.NamedResource("ivysaur", "https://pokeapi.co/api/v2/pokemon-species/2/"), ivysaurDetails, List.of());
        var bulbasaur = new PokeApiResponses.ChainLink(
                new PokeApiResponses.NamedResource("bulbasaur", "https://pokeapi.co/api/v2/pokemon-species/1/"), List.of(), List.of(ivysaur));
        return new PokeApiResponses.EvolutionChain(1, bulbasaur);
    }
}
