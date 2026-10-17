package com.lucasamaral.pokedex.web.collection;

import com.lucasamaral.pokedex.web.validation.PokemonKeyFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SyncRequest(
        @Schema(description = "Pokédex number or PokeAPI name", example = "pikachu")
        @NotBlank
        @Pattern(regexp = PokemonKeyFormat.PATTERN, message = PokemonKeyFormat.MESSAGE)
        String idOrName) {
}
