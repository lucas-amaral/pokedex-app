package com.lucasamaral.pokedex.web.collection;

import com.lucasamaral.pokedex.application.collection.BrowseCollectionUseCase;
import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.SyncPokemonUseCase;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.web.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pokemon")
@Tag(name = "Collection", description = "Pokémon copied into the local database, with proprietary fields")
class CollectionController {

    private final SyncPokemonUseCase syncPokemon;
    private final BrowseCollectionUseCase browseCollection;

    CollectionController(SyncPokemonUseCase syncPokemon, BrowseCollectionUseCase browseCollection) {
        this.syncPokemon = syncPokemon;
        this.browseCollection = browseCollection;
    }

    @PostMapping("/sync")
    @Operation(summary = "Add a Pokémon to the collection, or refresh its copy",
            description = "Copies the Pokémon from PokeAPI. A Pokémon already in the collection gets fresh source data "
                    + "and keeps its proprietary fields.")
    @ApiResponse(responseCode = "201", description = "Added to the collection; Location points to it")
    @ApiResponse(responseCode = "200", description = "Already in the collection; source data refreshed")
    @ApiResponse(responseCode = "400", description = "Missing or malformed number or name", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "PokeAPI has no Pokémon with that number or name", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "PokeAPI is unavailable or too slow", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<CollectedPokemonResponse> sync(@Valid @RequestBody SyncRequest request) {
        var result = syncPokemon.sync(request.idOrName());
        var body = CollectedPokemonResponse.from(result.pokemon());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/pokemon/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @Operation(summary = "Browse the collection", description = "Pokémon in Pokédex order, optionally filtered.")
    @ApiResponse(responseCode = "200", description = "One page of the collection")
    @ApiResponse(responseCode = "400", description = "Invalid page, size or filter", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<CollectedPokemonSummaryResponse> list(
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Pokémon per page")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size,
            @Parameter(description = "Part of the name, nickname or a localized name, case-insensitive")
            @RequestParam(required = false) @Size(max = 50) String name,
            @Parameter(description = "Exact tag")
            @RequestParam(required = false) @Size(max = 30) String tag,
            @Parameter(description = "Exact PokeAPI number, to check whether a Pokémon is already collected")
            @RequestParam(required = false) @Min(1) Integer pokeapiId) {
        var pokemon = browseCollection.list(new CollectionFilter(name, tag, pokeapiId), new PageQuery(page, size));
        return PageResponse.from(pokemon, CollectedPokemonSummaryResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one Pokémon of the collection")
    @ApiResponse(responseCode = "200", description = "The collected Pokémon")
    @ApiResponse(responseCode = "400", description = "The id is not a UUID", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No Pokémon with that id in the collection", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    CollectedPokemonResponse get(@PathVariable UUID id) {
        return CollectedPokemonResponse.from(browseCollection.get(id));
    }
}
