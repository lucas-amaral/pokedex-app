package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PokeApiMapperTest {

    private static final PokeApiResponses.NamedResource SPECIES = new PokeApiResponses.NamedResource("bulbasaur", null);

    @Test
    void leavesSpriteEmptyWhenPokeApiSendsNoSprites() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of(), true);

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).spriteUrl()).isNull();
    }

    @Test
    void leavesCategoryEmptyWhenPokeApiSendsNoGenera() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, new PokeApiResponses.Sprites(null, null), List.of(), List.of(), List.of(), true);

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(null)).category()).isNull();
    }

    @Test
    void returnsNoAbilitiesWhenPokeApiSendsNone() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, new PokeApiResponses.Sprites(null, null), null, List.of(), List.of(), true);

        assertThat(PokeApiMapper.toSummary(pokemon, speciesWithGenera(List.of())).abilities()).isEmpty();
    }

    private static PokeApiResponses.Species speciesWithGenera(List<PokeApiResponses.Genus> genera) {
        return new PokeApiResponses.Species(1, "bulbasaur", genera, List.of(), null);
    }

    @Test
    void leavesArtworkEmptyWhenPokeApiSendsNone() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES,
                new PokeApiResponses.Sprites(null, null), List.of(), List.of(), List.of(), true);

        assertThat(PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(List.of())).artworkUrl()).isNull();
    }

    @Test
    void leavesTheEvolutionConditionEmptyWhenPokeApiSendsNoDetails() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of(), true);

        var chain = PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(List.of())).evolutionChain();

        assertThat(chain.evolvesTo().getFirst().condition()).isNull();
    }

    @Test
    void keepsTheSpeciesOfAnAlternateForm() {
        var species = new PokeApiResponses.NamedResource("raichu", "https://pokeapi.co/api/v2/pokemon-species/26/");
        var alolanRaichu = new PokeApiResponses.Pokemon(10100, "raichu-alola", 7, 210, species, null, List.of(), List.of(), List.of(), false);

        var details = PokeApiMapper.toDetails(alolanRaichu, speciesWithGenera(List.of()), chainOf(List.of()));

        assertThat(details.name()).isEqualTo("raichu-alola");
        assertThat(details.speciesName()).isEqualTo("raichu");
    }

    @Test
    void marksAnAlternateForm() {
        var species = new PokeApiResponses.NamedResource("raichu", "https://pokeapi.co/api/v2/pokemon-species/26/");
        var alolanRaichu = new PokeApiResponses.Pokemon(10100, "raichu-alola", 7, 210, species, null, List.of(), List.of(), List.of(), false);

        assertThat(PokeApiMapper.toDetails(alolanRaichu, speciesWithGenera(List.of()), chainOf(List.of())).defaultForm()).isFalse();
    }

    @Test
    void treatsAPokemonWithoutTheDefaultFlagAsTheDefaultForm() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of(), null);

        assertThat(PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(List.of())).defaultForm()).isTrue();
    }

    @Test
    void fallsBackToTheFirstWayToEvolveWhenNoneIsMarkedDefault() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of(), true);
        var details = List.of(detail("level-up", 16, null, null), detail("use-item", null, "leaf-stone", null));

        var condition = PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(details))
                .evolutionChain().evolvesTo().getFirst().condition();

        assertThat(condition).isEqualTo(new EvolutionCondition("level-up", 16, null));
    }

    @Test
    void treatsAnEmptyTimeOfDayAsNoTimeOfDay() {
        var pokemon = new PokeApiResponses.Pokemon(1, "bulbasaur", 7, 69, SPECIES, null, List.of(), List.of(), List.of(), true);
        // PokeAPI sends "" rather than null when the time of day does not matter.
        var details = List.of(detail("level-up", 16, null, ""));

        var condition = PokeApiMapper.toDetails(pokemon, speciesWithGenera(List.of()), chainOf(details))
                .evolutionChain().evolvesTo().getFirst().condition();

        assertThat(condition.timeOfDay()).isNull();
    }

    private static PokeApiResponses.EvolutionDetail detail(String trigger, Integer minLevel, String item, String timeOfDay) {
        return new PokeApiResponses.EvolutionDetail(new PokeApiResponses.NamedResource(trigger, null), minLevel,
                item == null ? null : new PokeApiResponses.NamedResource(item, null), null, null, timeOfDay, null, null, null);
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
