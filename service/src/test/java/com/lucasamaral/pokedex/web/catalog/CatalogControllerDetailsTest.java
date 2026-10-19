package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.GetPokemonDetailsUseCase;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import com.lucasamaral.pokedex.infrastructure.config.JsonConfig;
import com.lucasamaral.pokedex.infrastructure.security.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, JsonConfig.class})
class CatalogControllerDetailsTest {

    private static final String URL = "/api/v1/catalog/pokemon/{idOrName}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BrowseCatalogUseCase browseCatalog;

    @MockitoBean
    private GetPokemonDetailsUseCase getPokemonDetails;

    @Test
    void returnsTheDetailView() {
        given(getPokemonDetails.getDetails("bulbasaur")).willReturn(aPokemonDetails().build());

        var response = mvc.get().uri(URL, "bulbasaur");

        assertThat(response).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {
                  "id": 1,
                  "name": "bulbasaur",
                  "speciesName": "bulbasaur",
                  "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                  "artworkUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
                  "category": "Seed Pokémon",
                  "description": "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.",
                  "heightM": 0.7,
                  "weightKg": 6.9,
                  "types": ["grass", "poison"],
                  "abilities": [{"name": "overgrow", "hidden": false}, {"name": "chlorophyll", "hidden": true}],
                  "stats": [
                    {"name": "hp", "value": 45}, {"name": "attack", "value": 49}, {"name": "defense", "value": 49},
                    {"name": "special-attack", "value": 65}, {"name": "special-defense", "value": 65}, {"name": "speed", "value": 45}
                  ],
                  "evolutionChain": {
                    "speciesId": 1, "name": "bulbasaur", "condition": null,
                    "evolvesTo": [{
                      "speciesId": 2, "name": "ivysaur",
                      "condition": {"trigger": "level-up", "minLevel": 16, "item": null},
                      "evolvesTo": [{
                        "speciesId": 3, "name": "venusaur",
                        "condition": {"trigger": "level-up", "minLevel": 32, "item": null},
                        "evolvesTo": []
                      }]
                    }]
                  }
                }
                """);
    }

    @Test
    void saysWhetherItIsTheDefaultFormAndWhatEachEvolutionNeeds() {
        var espeon = new EvolutionNode(196, "espeon", null, new EvolutionCondition("level-up", null, null, "soothe-bell", 160, "day", "psychic", "psychic"), List.of());
        var eevee = new EvolutionNode(133, "eevee", null, null, List.of(espeon));
        given(getPokemonDetails.getDetails("eevee-starter"))
                .willReturn(aPokemonDetails().withName("eevee-starter").withSpeciesName("eevee").withDefaultForm(false).withEvolutionChain(eevee).build());

        var response = mvc.get().uri(URL, "eevee-starter");

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {
                  "defaultForm": false,
                  "evolutionChain": {
                    "evolvesTo": [{
                      "condition": {
                        "trigger": "level-up", "minLevel": null, "item": null, "heldItem": "soothe-bell", "minHappiness": 160,
                        "timeOfDay": "day", "knownMove": "psychic", "knownMoveType": "psychic"
                      }
                    }]
                  }
                }
                """);
    }

    @Test
    void answersNotFoundForAnUnknownPokemon() {
        given(getPokemonDetails.getDetails("missingno")).willThrow(new PokemonNotFoundException("missingno"));

        var response = mvc.get().uri(URL, "missingno");

        assertThat(response)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Pokémon not found");
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("No Pokémon found for 'missingno'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pika chu", "mew!", "a-name-that-is-far-too-long-to-be-any-real-pokemon-name"})
    void rejectsAMalformedNumberOrName(String idOrName) {
        var response = mvc.get().uri(URL, idOrName);

        assertThat(response)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].parameter").isEqualTo("idOrName");
        verifyNoInteractions(getPokemonDetails);
    }

    @Test
    void answersServiceUnavailableWhenPokeApiIsDown() {
        given(getPokemonDetails.getDetails(any()))
                .willThrow(new CatalogUnavailableException("PokeAPI request failed", new IOException("boom")));

        assertThat(mvc.get().uri(URL, "pikachu")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
