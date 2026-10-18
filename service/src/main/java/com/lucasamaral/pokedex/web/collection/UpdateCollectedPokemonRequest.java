package com.lucasamaral.pokedex.web.collection;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The full set of proprietary fields (PUT replaces them all; a field left out is cleared) plus the version the
 * edit started from. Detailed rules (language tags, tag format) are checked by {@link CustomFields}.
 *
 * @param unknownFields anything else sent in the body, such as source data like "name"; refused by the controller
 */
public record UpdateCollectedPokemonRequest(
        @Schema(description = "Version of the Pokémon this edit started from", example = "0")
        @NotNull @PositiveOrZero Long version,
        @Schema(example = "Bulby") @Size(max = CustomFields.NICKNAME_MAX) String nickname,
        @Schema(example = "Our first starter") @Size(max = CustomFields.NOTES_MAX) String notes,
        @Schema(example = "Kanto") @Size(max = CustomFields.REGION_MAX) String region,
        @Schema(description = "Names by language tag", example = "{\"pt-BR\": \"Bulbassauro\"}")
        @Size(max = CustomFields.MAX_LANGUAGES) Map<String, String> localizedNames,
        @Schema(description = "Lowercase letters, digits and hyphens", example = "[\"starter\"]")
        @Size(max = CustomFields.MAX_TAGS) Set<String> tags,
        @Schema(hidden = true) @JsonAnySetter Map<String, Object> unknownFields) {

    public UpdateCollectedPokemonRequest {
        unknownFields = unknownFields == null ? Map.of() : new LinkedHashMap<>(unknownFields);
    }

    CustomFields toCustomFields() {
        return new CustomFields(nickname, notes, region, localizedNames, tags);
    }
}
