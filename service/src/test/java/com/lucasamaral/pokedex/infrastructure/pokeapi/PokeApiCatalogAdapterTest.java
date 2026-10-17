package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.PokemonSummary;
import com.lucasamaral.pokedex.domain.pokemon.Weight;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.lucasamaral.pokedex.support.Fixtures.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

@WireMockTest
class PokeApiCatalogAdapterTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);

    private ExecutorService executor;
    private PokeApiCatalogAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) {
        var properties = new PokeApiProperties(URI.create(wireMock.getHttpBaseUrl()), Duration.ofSeconds(1), READ_TIMEOUT);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        adapter = new PokeApiCatalogAdapter(PokeApiConfig.createClient(RestClient.builder(), properties), executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void mapsEveryEntryOfThePageInPokedexOrder() {
        stubFirstTwoPokemon();

        var page = adapter.findSummaries(new PageQuery(0, 2));

        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.content()).extracting(PokemonSummary::id, PokemonSummary::name)
                .containsExactly(tuple(1, "bulbasaur"), tuple(2, "ivysaur"));
    }

    @Test
    void mapsSpriteCategoryWeightAndAbilities() {
        stubFirstTwoPokemon();

        var bulbasaur = adapter.findSummaries(new PageQuery(0, 2)).content().getFirst();

        assertThat(bulbasaur.spriteUrl())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.weight()).isEqualTo(Weight.ofHectograms(69));
        assertThat(bulbasaur.abilities())
                .containsExactly(new Ability("overgrow", false), new Ability("chlorophyll", true));
    }

    @Test
    void asksPokeApiForTheOffsetAndLimitOfTheQuery() {
        stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(okJson("{\"count\":1351,\"results\":[]}")));

        var page = adapter.findSummaries(new PageQuery(3, 20));

        assertThat(page.content()).isEmpty();
        verify(getRequestedFor(urlPathEqualTo("/pokemon"))
                .withQueryParam("offset", equalTo("60"))
                .withQueryParam("limit", equalTo("20")));
    }

    @Test
    void leavesCategoryEmptyWhenThereIsNoEnglishGenus() {
        stubFirstTwoPokemon();
        stubFor(get("/pokemon-species/bulbasaur").willReturn(okJson("""
                {"id": 1, "name": "bulbasaur", "genera": [{"genus": "たねポケモン", "language": {"name": "ja"}}]}
                """)));

        var bulbasaur = adapter.findSummaries(new PageQuery(0, 2)).content().getFirst();

        assertThat(bulbasaur.category()).isNull();
    }

    @Test
    void reportsCatalogUnavailableWhenTheListFails() {
        stubFor(get(urlPathEqualTo("/pokemon")).willReturn(serverError()));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> adapter.findSummaries(new PageQuery(0, 2)));
    }

    @Test
    void reportsCatalogUnavailableWhenOneEntryFails() {
        stubFirstTwoPokemon();
        stubFor(get("/pokemon/ivysaur").willReturn(serverError()));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> adapter.findSummaries(new PageQuery(0, 2)));
    }

    @Test
    void reportsCatalogUnavailableWhenPokeApiIsTooSlow() {
        stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(okJson(read("pokeapi/list-offset-0-limit-2.json"))
                        .withFixedDelay((int) READ_TIMEOUT.multipliedBy(3).toMillis())));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> adapter.findSummaries(new PageQuery(0, 2)));
    }

    private static void stubFirstTwoPokemon() {
        stubFor(get(urlPathEqualTo("/pokemon")).willReturn(okJson(read("pokeapi/list-offset-0-limit-2.json"))));
        for (var name : new String[]{"bulbasaur", "ivysaur"}) {
            stubFor(get("/pokemon/" + name).willReturn(okJson(read("pokeapi/pokemon-" + name + ".json"))));
            stubFor(get("/pokemon-species/" + name).willReturn(okJson(read("pokeapi/pokemon-species-" + name + ".json"))));
        }
    }
}
