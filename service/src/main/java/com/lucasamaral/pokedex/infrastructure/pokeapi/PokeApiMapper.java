package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import com.lucasamaral.pokedex.domain.pokemon.Weight;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Translates PokeAPI's JSON shapes into domain objects.
 */
final class PokeApiMapper {

    private static final String ENGLISH = "en";
    private static final String SPRITE_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";
    private static final Pattern WRAPPED_HYPHEN = Pattern.compile("­\\s*");
    private static final Pattern LINE_BREAKS = Pattern.compile("\\s+");

    private PokeApiMapper() {
    }

    static PokemonSummary toSummary(PokeApiResponses.Pokemon pokemon, PokeApiResponses.Species species) {
        return new PokemonSummary(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites() == null ? null : pokemon.sprites().frontDefault(),
                englishGenus(species),
                Weight.ofHectograms(pokemon.weight()),
                abilities(pokemon));
    }

    static PokemonDetails toDetails(
            PokeApiResponses.Pokemon pokemon, PokeApiResponses.Species species, PokeApiResponses.EvolutionChain chain) {
        return new PokemonDetails(
                pokemon.id(),
                pokemon.name(),
                pokemon.species().name(),
                artwork(pokemon),
                englishGenus(species),
                englishDescription(species),
                Height.ofDecimeters(pokemon.height()),
                Weight.ofHectograms(pokemon.weight()),
                types(pokemon),
                abilities(pokemon),
                stats(pokemon),
                evolutionNode(chain.chain(), true));
    }

    /** PokeAPI links end in the resource id: ".../evolution-chain/67/" → 67. */
    static int idFromUrl(String url) {
        var trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(trimmed.substring(trimmed.lastIndexOf('/') + 1));
    }

    private static String artwork(PokeApiResponses.Pokemon pokemon) {
        var sprites = pokemon.sprites();
        if (sprites == null || sprites.other() == null || sprites.other().officialArtwork() == null) {
            return null;
        }
        return sprites.other().officialArtwork().frontDefault();
    }

    private static String englishGenus(PokeApiResponses.Species species) {
        return lastEnglish(species.genera(), PokeApiResponses.Genus::language, PokeApiResponses.Genus::genus);
    }

    /**
     * The most recent English Pokédex entry. PokeAPI keeps the games' hard line breaks, form feeds
     * and soft hyphens, so the text is joined back onto one line.
     */
    private static String englishDescription(PokeApiResponses.Species species) {
        var text = lastEnglish(species.flavorTextEntries(), PokeApiResponses.FlavorText::language, PokeApiResponses.FlavorText::text);
        if (text == null) {
            return null;
        }
        var unhyphenated = WRAPPED_HYPHEN.matcher(text).replaceAll("");
        return LINE_BREAKS.matcher(unhyphenated).replaceAll(" ").strip();
    }

    private static <T> String lastEnglish(
            List<T> entries, Function<T, PokeApiResponses.NamedResource> language, Function<T, String> value) {
        if (entries == null) {
            return null;
        }
        return entries.stream()
                .filter(entry -> ENGLISH.equals(language.apply(entry).name()))
                .map(value)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private static List<Ability> abilities(PokeApiResponses.Pokemon pokemon) {
        if (pokemon.abilities() == null) {
            return List.of();
        }
        return pokemon.abilities().stream()
                .sorted(Comparator.comparingInt(PokeApiResponses.PokemonAbility::slot))
                .map(entry -> new Ability(entry.ability().name(), entry.hidden()))
                .toList();
    }

    private static List<String> types(PokeApiResponses.Pokemon pokemon) {
        return Objects.requireNonNullElse(pokemon.types(), List.<PokeApiResponses.PokemonType>of()).stream()
                .sorted(Comparator.comparingInt(PokeApiResponses.PokemonType::slot))
                .map(entry -> entry.type().name())
                .toList();
    }

    private static List<BaseStat> stats(PokeApiResponses.Pokemon pokemon) {
        return Objects.requireNonNullElse(pokemon.stats(), List.<PokeApiResponses.PokemonStat>of()).stream()
                .map(entry -> new BaseStat(entry.stat().name(), entry.baseStat()))
                .toList();
    }

    private static EvolutionNode evolutionNode(PokeApiResponses.ChainLink link, boolean isBaseForm) {
        var speciesId = idFromUrl(link.species().url());
        var children = Objects.requireNonNullElse(link.evolvesTo(), List.<PokeApiResponses.ChainLink>of()).stream()
                .map(child -> evolutionNode(child, false))
                .toList();
        return new EvolutionNode(
                speciesId,
                link.species().name(),
                SPRITE_URL.formatted(speciesId),
                isBaseForm ? null : condition(link),
                children);
    }

    /** A species can evolve in several ways; the first one PokeAPI lists is shown. */
    private static EvolutionCondition condition(PokeApiResponses.ChainLink link) {
        if (link.evolutionDetails() == null || link.evolutionDetails().isEmpty()) {
            return null;
        }
        var detail = link.evolutionDetails().getFirst();
        return new EvolutionCondition(
                detail.trigger().name(),
                detail.minLevel(),
                detail.item() == null ? null : detail.item().name());
    }
}
