package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.PokemonCatalog;
import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import com.lucasamaral.pokedex.support.PostgresTestConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.lucasamaral.pokedex.support.Fixtures.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class PokeApiCachingTest {

    @RegisterExtension
    static final WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void pointToWireMock(DynamicPropertyRegistry registry) {
        registry.add("pokedex.pokeapi.base-url", pokeApi::baseUrl);
    }

    @Autowired
    private PokemonCatalog catalog;

    @Autowired
    private PokemonDetailsCatalog detailsCatalog;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void startEmpty() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void servesARepeatedPageWithoutCallingPokeApiAgain() {
        stubFirstTwoPokemon();

        var first = catalog.findSummaries(new PageQuery(0, 2));
        var second = catalog.findSummaries(new PageQuery(0, 2));

        assertThat(second).isEqualTo(first);
        pokeApi.verify(exactly(1), getRequestedFor(urlPathEqualTo("/pokemon")));
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/bulbasaur")));
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/bulbasaur")));
    }

    @Test
    void reusesCachedPokemonAcrossDifferentPageSizes() {
        stubFirstTwoPokemon();

        catalog.findSummaries(new PageQuery(0, 2));
        catalog.findSummaries(new PageQuery(0, 1));

        pokeApi.verify(exactly(2), getRequestedFor(urlPathEqualTo("/pokemon")));
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/bulbasaur")));
    }

    @Test
    void doesNotCacheFailures() {
        stubFirstTwoPokemon();
        pokeApi.stubFor(get("/pokemon/ivysaur").willReturn(serverError()));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> catalog.findSummaries(new PageQuery(0, 2)));

        pokeApi.stubFor(get("/pokemon/ivysaur").willReturn(okJson(read("pokeapi/pokemon-ivysaur.json"))));
        assertThat(catalog.findSummaries(new PageQuery(0, 2)).content()).hasSize(2);
    }

    @Test
    void servesRepeatedDetailsWithoutCallingPokeApiAgain() {
        stubFirstTwoPokemon();
        pokeApi.stubFor(get("/evolution-chain/1").willReturn(okJson(read("pokeapi/evolution-chain-1.json"))));

        var first = detailsCatalog.findDetails("bulbasaur");
        var second = detailsCatalog.findDetails("bulbasaur");

        assertThat(second).isEqualTo(first);
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/bulbasaur")));
        pokeApi.verify(exactly(1), getRequestedFor(urlEqualTo("/evolution-chain/1")));
    }

    private static void stubFirstTwoPokemon() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon")).willReturn(okJson(read("pokeapi/list-offset-0-limit-2.json"))));
        for (var name : new String[]{"bulbasaur", "ivysaur"}) {
            pokeApi.stubFor(get("/pokemon/" + name).willReturn(okJson(read("pokeapi/pokemon-" + name + ".json"))));
            pokeApi.stubFor(get("/pokemon-species/" + name).willReturn(okJson(read("pokeapi/pokemon-species-" + name + ".json"))));
        }
    }
}
