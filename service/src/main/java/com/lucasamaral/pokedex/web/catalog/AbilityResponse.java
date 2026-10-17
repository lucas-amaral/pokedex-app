package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.domain.pokemon.Ability;

public record AbilityResponse(String name, boolean hidden) {

    public static AbilityResponse from(Ability ability) {
        return new AbilityResponse(ability.name(), ability.hidden());
    }
}
