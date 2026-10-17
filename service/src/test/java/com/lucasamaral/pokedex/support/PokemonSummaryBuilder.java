package com.lucasamaral.pokedex.support;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.util.List;

/**
 * Test Data Builder: a valid Bulbasaur by default, so each test only states what it cares about.
 */
public final class PokemonSummaryBuilder {

    private int id = 1;
    private String name = "bulbasaur";
    private String spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png";
    private String category = "Seed Pokémon";
    private Weight weight = Weight.ofHectograms(69);
    private List<Ability> abilities = List.of(new Ability("overgrow", false), new Ability("chlorophyll", true));

    private PokemonSummaryBuilder() {
    }

    public static PokemonSummaryBuilder aPokemonSummary() {
        return new PokemonSummaryBuilder();
    }

    public PokemonSummaryBuilder withId(int id) {
        this.id = id;
        return this;
    }

    public PokemonSummaryBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public PokemonSummaryBuilder withSpriteUrl(String spriteUrl) {
        this.spriteUrl = spriteUrl;
        return this;
    }

    public PokemonSummaryBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public PokemonSummaryBuilder withWeight(Weight weight) {
        this.weight = weight;
        return this;
    }

    public PokemonSummaryBuilder withAbilities(List<Ability> abilities) {
        this.abilities = abilities;
        return this;
    }

    public PokemonSummary build() {
        return new PokemonSummary(id, name, spriteUrl, category, weight, abilities);
    }
}
