package com.lucasamaral.pokedex.domain.collection;

import com.lucasamaral.pokedex.domain.InvalidFieldException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CustomFieldsTest {

    @Test
    void startsEmpty() {
        var empty = CustomFields.empty();

        assertThat(empty.nickname()).isNull();
        assertThat(empty.notes()).isNull();
        assertThat(empty.region()).isNull();
        assertThat(empty.localizedNames()).isEmpty();
        assertThat(empty.tags()).isEmpty();
    }

    @Test
    void trimsTextAndTreatsBlankAsAbsent() {
        var fields = new CustomFields("  Bulby ", "   ", " Kanto ", Map.of(), Set.of());

        assertThat(fields.nickname()).isEqualTo("Bulby");
        assertThat(fields.notes()).isNull();
        assertThat(fields.region()).isEqualTo("Kanto");
    }

    @Test
    void normalizesTagsToLowercaseAndSortsThem() {
        var fields = new CustomFields(null, null, null, Map.of(), Set.of(" Starter", "grass-type", "KANTO"));

        assertThat(fields.tags()).containsExactly("grass-type", "kanto", "starter");
    }

    @Test
    void keepsLocalizedNamesByLanguageTag() {
        var names = new LinkedHashMap<String, String>();
        names.put("pt-BR", " Bulbassauro ");
        names.put("ja", "フシギダネ");

        var fields = new CustomFields(null, null, null, names, Set.of());

        assertThat(fields.localizedNames()).containsExactly(Map.entry("ja", "フシギダネ"), Map.entry("pt-BR", "Bulbassauro"));
    }

    @Test
    void rejectsANicknameThatIsTooLong() {
        assertInvalid("nickname", () -> new CustomFields("n".repeat(CustomFields.NICKNAME_MAX + 1), null, null, Map.of(), Set.of()));
    }

    @Test
    void rejectsNotesThatAreTooLong() {
        assertInvalid("notes", () -> new CustomFields(null, "n".repeat(CustomFields.NOTES_MAX + 1), null, Map.of(), Set.of()));
    }

    @Test
    void rejectsARegionThatIsTooLong() {
        assertInvalid("region", () -> new CustomFields(null, null, "r".repeat(CustomFields.REGION_MAX + 1), Map.of(), Set.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"has space", "ünïcode", "under_score", ""})
    void rejectsMalformedTags(String tag) {
        assertInvalid("tags", () -> new CustomFields(null, null, null, Map.of(), Set.of(tag)));
    }

    @Test
    void rejectsTooManyTags() {
        var tags = new java.util.HashSet<String>();
        for (int i = 0; i <= CustomFields.MAX_TAGS; i++) {
            tags.add("tag-" + i);
        }
        assertInvalid("tags", () -> new CustomFields(null, null, null, Map.of(), tags));
    }

    @ParameterizedTest
    @ValueSource(strings = {"portuguese", "PT", "pt_BR", "pt-br"})
    void rejectsMalformedLanguageTags(String language) {
        assertInvalid("localizedNames", () -> new CustomFields(null, null, null, Map.of(language, "Bulbassauro"), Set.of()));
    }

    @Test
    void rejectsABlankLocalizedName() {
        assertInvalid("localizedNames", () -> new CustomFields(null, null, null, Map.of("pt-BR", "  "), Set.of()));
    }

    private static void assertInvalid(String field, Runnable creation) {
        assertThatExceptionOfType(InvalidFieldException.class)
                .isThrownBy(creation::run)
                .satisfies(exception -> assertThat(exception.field()).isEqualTo(field));
    }
}
