package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
import com.lucasamaral.pokedex.application.catalog.GetPokemonDetailsUseCase;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.web.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/pokemon")
@Tag(name = "Catalog", description = "Pokémon served live from PokeAPI")
class CatalogController {

    private static final String ID_OR_NAME = "[A-Za-z0-9-]{1,50}";

    private final BrowseCatalogUseCase browseCatalog;
    private final GetPokemonDetailsUseCase getPokemonDetails;

    CatalogController(BrowseCatalogUseCase browseCatalog, GetPokemonDetailsUseCase getPokemonDetails) {
        this.browseCatalog = browseCatalog;
        this.getPokemonDetails = getPokemonDetails;
    }

    @GetMapping
    @Operation(summary = "Browse the catalog",
            description = "One page of Pokémon in Pokédex order, with sprite, category, weight and abilities.")
    @ApiResponse(responseCode = "200", description = "One page of the catalog")
    @ApiResponse(responseCode = "400", description = "Invalid page or size",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "PokeAPI is unavailable or too slow",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<PokemonSummaryResponse> browse(
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Pokémon per page")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(browseCatalog.browse(new PageQuery(page, size)), PokemonSummaryResponse::from);
    }

    @GetMapping("/{idOrName}")
    @Operation(summary = "Get one Pokémon",
            description = "Artwork, base stats, Pokédex description and evolution chain, by Pokédex number or name.")
    @ApiResponse(responseCode = "200", description = "The Pokémon")
    @ApiResponse(responseCode = "400", description = "Malformed number or name",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No Pokémon with that number or name",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "PokeAPI is unavailable or too slow",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    PokemonDetailsResponse details(
            @Parameter(description = "Pokédex number (25) or name (pikachu)", example = "pikachu")
            @PathVariable @Pattern(regexp = ID_OR_NAME, message = "must be a Pokédex number or a name made of letters, digits and hyphens")
            String idOrName) {
        return PokemonDetailsResponse.from(getPokemonDetails.getDetails(idOrName));
    }
}
