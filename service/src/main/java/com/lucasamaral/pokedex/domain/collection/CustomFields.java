package com.lucasamaral.pokedex.domain.collection;

import com.lucasamaral.pokedex.domain.InvalidFieldException;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The proprietary fields of a collected Pokémon: data PokeAPI does not have and only we can change.
 * <p>
 * Text is trimmed and blank text counts as absent. Tags are lowercased and sorted; localized names are
 * keyed by language tag ("pt-BR", "ja") and sorted by it.
 */
public record CustomFields(
        String nickname,
        String notes,
        String region,
        Map<String, String> localizedNames,
        Set<String> tags) {

    public static final int NICKNAME_MAX = 30;
    public static final int NOTES_MAX = 500;
    public static final int REGION_MAX = 50;
    public static final int LOCALIZED_NAME_MAX = 50;
    public static final int MAX_LANGUAGES = 10;
    public static final int TAG_MAX = 30;
    public static final int MAX_TAGS = 10;

    private static final Pattern LANGUAGE_TAG = Pattern.compile("[a-z]{2,3}(-[A-Z]{2})?");
    private static final Pattern TAG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private static final CustomFields EMPTY = new CustomFields(null, null, null, Map.of(), Set.of());

    public CustomFields {
        nickname = optionalText("nickname", nickname, NICKNAME_MAX);
        notes = optionalText("notes", notes, NOTES_MAX);
        region = optionalText("region", region, REGION_MAX);
        localizedNames = normalizeLocalizedNames(Objects.requireNonNullElse(localizedNames, Map.of()));
        tags = normalizeTags(Objects.requireNonNullElse(tags, Set.of()));
    }

    public static CustomFields empty() {
        return EMPTY;
    }

    private static String optionalText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidFieldException(field, "must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static SortedMap<String, String> normalizeLocalizedNames(Map<String, String> names) {
        if (names.size() > MAX_LANGUAGES) {
            throw new InvalidFieldException("localizedNames", "must have at most " + MAX_LANGUAGES + " languages");
        }
        var normalized = new TreeMap<String, String>();
        names.forEach((language, name) -> {
            if (language == null || !LANGUAGE_TAG.matcher(language).matches()) {
                throw new InvalidFieldException("localizedNames", "language must look like 'pt-BR' or 'ja', was '" + language + "'");
            }
            var text = optionalText("localizedNames", name, LOCALIZED_NAME_MAX);
            if (text == null) {
                throw new InvalidFieldException("localizedNames", "name for '" + language + "' must not be blank");
            }
            normalized.put(language, text);
        });
        return Collections.unmodifiableSortedMap(normalized);
    }

    private static SortedSet<String> normalizeTags(Set<String> tags) {
        if (tags.size() > MAX_TAGS) {
            throw new InvalidFieldException("tags", "must have at most " + MAX_TAGS + " tags");
        }
        var normalized = new TreeSet<String>();
        for (var tag : tags) {
            var value = tag == null ? "" : tag.strip().toLowerCase(Locale.ROOT);
            if (value.length() > TAG_MAX || !TAG.matcher(value).matches()) {
                throw new InvalidFieldException("tags", "must be lowercase letters, digits and hyphens, at most " + TAG_MAX
                        + " characters, was '" + tag + "'");
            }
            normalized.add(value);
        }
        return Collections.unmodifiableSortedSet(normalized);
    }
}
