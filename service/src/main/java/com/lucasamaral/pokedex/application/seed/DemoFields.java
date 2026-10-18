package com.lucasamaral.pokedex.application.seed;

import com.lucasamaral.pokedex.domain.collection.CustomFields;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Proprietary fields for the demo collection, so the region, tags and localized names have something to show:
 * the home region of each generation, a generation tag, the starters, legendaries and mythicals, and the starters'
 * names in Japanese, French and German.
 */
final class DemoFields {

    private static final int LAST_OF_GENERATION_1 = 151;
    private static final int LAST_OF_GENERATION_2 = 251;

    private static final Set<Integer> STARTERS = Set.of(1, 4, 7, 152, 155, 158);
    private static final Set<Integer> LEGENDARIES = Set.of(144, 145, 146, 150, 243, 244, 245, 249, 250);
    private static final Set<Integer> MYTHICALS = Set.of(151, 251);

    private static final Map<Integer, Map<String, String>> LOCALIZED_NAMES = Map.of(
            1, Map.of("ja", "フシギダネ", "fr", "Bulbizarre", "de", "Bisasam"),
            4, Map.of("ja", "ヒトカゲ", "fr", "Salamèche", "de", "Glumanda"),
            7, Map.of("ja", "ゼニガメ", "fr", "Carapuce", "de", "Schiggy"),
            25, Map.of("ja", "ピカチュウ", "fr", "Pikachu", "de", "Pikachu"),
            152, Map.of("ja", "チコリータ", "fr", "Germignon", "de", "Endivie"),
            155, Map.of("ja", "ヒノアラシ", "fr", "Héricendre", "de", "Feurigel"),
            158, Map.of("ja", "ワニノコ", "fr", "Kaiminus", "de", "Karnimani"));

    private DemoFields() {
    }

    static CustomFields forPokemon(int pokeapiId) {
        return new CustomFields(null, null, region(pokeapiId), LOCALIZED_NAMES.getOrDefault(pokeapiId, Map.of()),
                tags(pokeapiId));
    }

    private static String region(int pokeapiId) {
        if (pokeapiId <= LAST_OF_GENERATION_1) {
            return "Kanto";
        }
        return pokeapiId <= LAST_OF_GENERATION_2 ? "Johto" : null;
    }

    private static Set<String> tags(int pokeapiId) {
        var tags = new HashSet<String>();
        if (pokeapiId <= LAST_OF_GENERATION_1) {
            tags.add("gen-1");
        } else if (pokeapiId <= LAST_OF_GENERATION_2) {
            tags.add("gen-2");
        }
        if (STARTERS.contains(pokeapiId)) {
            tags.add("starter");
        }
        if (LEGENDARIES.contains(pokeapiId)) {
            tags.add("legendary");
        }
        if (MYTHICALS.contains(pokeapiId)) {
            tags.add("mythical");
        }
        return tags;
    }
}
