package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import com.lucasamaral.pokedex.domain.collection.SourceData;
import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

/**
 * Copies between the domain aggregate and its JPA entity, in both directions.
 */
final class PokemonEntityMapper {

    private PokemonEntityMapper() {
    }

    static CollectedPokemon toDomain(PokemonEntity entity) {
        var source = new SourceData(
                entity.pokeapiId,
                entity.name,
                entity.speciesName,
                entity.category,
                entity.description,
                entity.spriteUrl,
                entity.artworkUrl,
                Height.ofDecimeters(entity.heightDm),
                Weight.ofHectograms(entity.weightHg),
                entity.types,
                entity.abilities.stream().map(ability -> new Ability(ability.name(), ability.hidden())).toList(),
                entity.stats.stream().map(stat -> new BaseStat(stat.name(), stat.baseValue())).toList());
        var custom = new CustomFields(entity.nickname, entity.notes, entity.region, entity.localizedNames, entity.tags);
        return new CollectedPokemon(entity.id, source, custom, entity.syncedAt);
    }

    /** Writes the aggregate into a new or managed entity; collections are replaced in place so JPA tracks them. */
    static void copyInto(CollectedPokemon pokemon, PokemonEntity entity) {
        var source = pokemon.source();
        var custom = pokemon.custom();
        entity.id = pokemon.id();
        entity.pokeapiId = source.pokeapiId();
        entity.name = source.name();
        entity.speciesName = source.speciesName();
        entity.category = source.category();
        entity.description = source.description();
        entity.spriteUrl = source.spriteUrl();
        entity.artworkUrl = source.artworkUrl();
        entity.heightDm = source.height().decimeters();
        entity.weightHg = source.weight().hectograms();
        replace(entity.types, source.types());
        replace(entity.abilities, source.abilities().stream()
                .map(ability -> new PokemonEntity.AbilityValue(ability.name(), ability.hidden())).toList());
        replace(entity.stats, source.stats().stream()
                .map(stat -> new PokemonEntity.StatValue(stat.name(), stat.value())).toList());
        entity.nickname = custom.nickname();
        entity.notes = custom.notes();
        entity.region = custom.region();
        entity.localizedNames.clear();
        entity.localizedNames.putAll(custom.localizedNames());
        entity.tags.clear();
        entity.tags.addAll(custom.tags());
        entity.syncedAt = pokemon.syncedAt();
    }

    private static <T> void replace(java.util.List<T> target, java.util.List<T> values) {
        target.clear();
        target.addAll(values);
    }
}
