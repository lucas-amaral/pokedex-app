package com.lucasamaral.pokedex.support;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.util.List;

/**
 * Test Data Builder: a valid Bulbasaur detail view by default.
 */
public final class PokemonDetailsBuilder {

    private int id = 1;
    private String name = "bulbasaur";
    private String speciesName = "bulbasaur";
    private String spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png";
    private String artworkUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";
    private String category = "Seed Pokémon";
    private String description = "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.";
    private Height height = Height.ofDecimeters(7);
    private Weight weight = Weight.ofHectograms(69);
    private List<String> types = List.of("grass", "poison");
    private List<Ability> abilities = List.of(new Ability("overgrow", false), new Ability("chlorophyll", true));
    private List<BaseStat> stats = List.of(
            new BaseStat("hp", 45), new BaseStat("attack", 49), new BaseStat("defense", 49),
            new BaseStat("special-attack", 65), new BaseStat("special-defense", 65), new BaseStat("speed", 45));
    private EvolutionNode evolutionChain = new EvolutionNode(1, "bulbasaur", null, null, List.of(
            new EvolutionNode(2, "ivysaur", null, new EvolutionCondition("level-up", 16, null), List.of(
                    new EvolutionNode(3, "venusaur", null, new EvolutionCondition("level-up", 32, null), List.of())))));

    private boolean defaultForm = true;

    private PokemonDetailsBuilder() {
    }

    public static PokemonDetailsBuilder aPokemonDetails() {
        return new PokemonDetailsBuilder();
    }

    public PokemonDetailsBuilder withId(int id) {
        this.id = id;
        return this;
    }

    public PokemonDetailsBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public PokemonDetailsBuilder withSpeciesName(String speciesName) {
        this.speciesName = speciesName;
        return this;
    }

    public PokemonDetailsBuilder withSpriteUrl(String spriteUrl) {
        this.spriteUrl = spriteUrl;
        return this;
    }

    public PokemonDetailsBuilder withWeight(Weight weight) {
        this.weight = weight;
        return this;
    }

    public PokemonDetailsBuilder withArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
        return this;
    }

    public PokemonDetailsBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public PokemonDetailsBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public PokemonDetailsBuilder withTypes(List<String> types) {
        this.types = types;
        return this;
    }

    public PokemonDetailsBuilder withEvolutionChain(EvolutionNode evolutionChain) {
        this.evolutionChain = evolutionChain;
        return this;
    }

    public PokemonDetailsBuilder withDefaultForm(boolean defaultForm) {
        this.defaultForm = defaultForm;
        return this;
    }

    public PokemonDetails build() {
        return new PokemonDetails(id, name, speciesName, spriteUrl, artworkUrl, category, description, height, weight, types, abilities, stats,
                evolutionChain, defaultForm);
    }
}
