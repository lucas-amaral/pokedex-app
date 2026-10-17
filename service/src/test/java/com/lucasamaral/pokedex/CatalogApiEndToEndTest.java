package com.lucasamaral.pokedex;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.context.annotation.Import;
import com.lucasamaral.pokedex.support.PostgresTestConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.lucasamaral.pokedex.support.Fixtures.read;

/**
 * US01 and US02 through the whole stack: real HTTP server, controller, use case, PokeAPI adapter and cache.
 * Only PokeAPI itself is replaced, by WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
@AutoConfigureRestTestClient
class CatalogApiEndToEndTest {

    private static final String CATALOG = "/api/v1/catalog/pokemon";

    @RegisterExtension
    static final WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void pointToWireMock(DynamicPropertyRegistry registry) {
        registry.add("pokedex.pokeapi.base-url", pokeApi::baseUrl);
    }

    @Autowired
    private RestTestClient client;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void startWithEmptyCache() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void browsesTheCatalogPageByPage() {
        stubFirstTwoPokemon();

        client.get().uri(CATALOG + "?page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().json("""
                        {
                          "content": [
                            {"id": 1, "name": "bulbasaur", "category": "Seed Pokémon", "weightKg": 6.9,
                             "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                             "abilities": [{"name": "overgrow", "hidden": false}, {"name": "chlorophyll", "hidden": true}]},
                            {"id": 2, "name": "ivysaur", "category": "Seed Pokémon", "weightKg": 13.0}
                          ],
                          "page": 0, "size": 2, "totalElements": 1351, "totalPages": 676, "hasNext": true
                        }
                        """);
    }

    @Test
    void answersServiceUnavailableWhenPokeApiIsDown() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon")).willReturn(serviceUnavailable()));

        client.get().uri(CATALOG)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Catalog unavailable");
    }

    @Test
    void rejectsAPageSizeAboveTheLimit() {
        client.get().uri(CATALOG + "?size=51")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.errors[0].parameter").isEqualTo("size");
    }

    @Test
    void showsTheDetailsOfOnePokemonWhateverTheCaseOfItsName() {
        stubFirstTwoPokemon();
        pokeApi.stubFor(get("/evolution-chain/1").willReturn(okJson(read("pokeapi/evolution-chain-1.json"))));

        client.get().uri(CATALOG + "/Bulbasaur")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        {
                          "id": 1, "name": "bulbasaur", "category": "Seed Pokémon", "heightM": 0.7, "weightKg": 6.9,
                          "types": ["grass", "poison"],
                          "description": "While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.",
                          "evolutionChain": {
                            "name": "bulbasaur", "condition": null,
                            "evolvesTo": [{"name": "ivysaur", "condition": {"trigger": "level-up", "minLevel": 16},
                                           "evolvesTo": [{"name": "venusaur", "condition": {"minLevel": 32}, "evolvesTo": []}]}]
                          }
                        }
                        """);
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/bulbasaur")));
    }

    @Test
    void answersNotFoundForAPokemonPokeApiDoesNotKnow() {
        pokeApi.stubFor(get("/pokemon/missingno").willReturn(notFound()));

        client.get().uri(CATALOG + "/missingno")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Pokémon not found");
    }

    @Test
    void rejectsAMalformedNameWithoutCallingPokeApi() {
        client.get().uri(CATALOG + "/mew!")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].parameter").isEqualTo("idOrName");

        pokeApi.verify(exactly(0), getRequestedFor(urlEqualTo("/pokemon/mew!")));
    }

    private static void stubFirstTwoPokemon() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon")).willReturn(okJson(read("pokeapi/list-offset-0-limit-2.json"))));
        for (var name : new String[]{"bulbasaur", "ivysaur"}) {
            pokeApi.stubFor(get("/pokemon/" + name).willReturn(okJson(read("pokeapi/pokemon-" + name + ".json"))));
            pokeApi.stubFor(get("/pokemon-species/" + name).willReturn(okJson(read("pokeapi/pokemon-species-" + name + ".json"))));
        }
    }
}
