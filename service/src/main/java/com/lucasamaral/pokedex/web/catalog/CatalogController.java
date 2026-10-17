package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/pokemon")
@Tag(name = "Catalog", description = "Pokémon served live from PokeAPI")
class CatalogController {

    private final BrowseCatalogUseCase browseCatalog;

    CatalogController(BrowseCatalogUseCase browseCatalog) {
        this.browseCatalog = browseCatalog;
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
}
