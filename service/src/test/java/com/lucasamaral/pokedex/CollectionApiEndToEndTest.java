package com.lucasamaral.pokedex;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import com.lucasamaral.pokedex.support.PostgresTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.lucasamaral.pokedex.support.Fixtures.read;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * US03 and US04 through the whole stack: real HTTP port, controller, use cases, PokeAPI adapter, JPA adapter and a real
 * PostgreSQL (Testcontainers). Only PokeAPI is replaced, by WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
@AutoConfigureRestTestClient
class CollectionApiEndToEndTest {

    private static final String COLLECTION = "/api/v1/pokemon";

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
    private CollectionRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void startEmpty() {
        jdbc.execute("TRUNCATE pokemon CASCADE");
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void addsAPokemonThenFindsItByItsLocationAndInTheList() {
        stubBulbasaur(69);

        var location = client.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idOrName\": \"bulbasaur\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.name").isEqualTo("bulbasaur")
                .returnResult().getResponseHeaders().getLocation();

        client.get().uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        {"pokeapiId": 1, "name": "bulbasaur", "category": "Seed Pokémon", "weightKg": 6.9,
                         "types": ["grass", "poison"], "nickname": null, "tags": []}
                        """);
        client.get().uri(COLLECTION)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].name").isEqualTo("bulbasaur");
    }

    @Test
    void reSyncRefreshesSourceDataAndKeepsProprietaryFields() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");
        var stored = repository.findById(id).orElseThrow();
        repository.save(stored.withCustomFields(
                new CustomFields("Bulby", "Our first starter", "Kanto", Map.of("pt-BR", "Bulbassauro"), Set.of("starter"))));
        stubBulbasaur(70);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        client.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idOrName\": \"1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        {"id": "%s", "weightKg": 7.0, "nickname": "Bulby", "notes": "Our first starter",
                         "region": "Kanto", "localizedNames": {"pt-BR": "Bulbassauro"}, "tags": ["starter"]}
                        """.formatted(id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pokemon", Integer.class)).isEqualTo(1);
    }

    @Test
    void filtersTheCollectionByTag() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");
        repository.save(repository.findById(id).orElseThrow()
                .withCustomFields(new CustomFields(null, null, null, Map.of(), Set.of("starter"))));

        client.get().uri(COLLECTION + "?tag=starter").exchange()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
        client.get().uri(COLLECTION + "?tag=legendary").exchange()
                .expectBody().jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void storesNothingForAPokemonPokeApiDoesNotKnow() {
        pokeApi.stubFor(get("/pokemon/missingno").willReturn(notFound()));

        client.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idOrName\": \"missingno\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pokemon", Integer.class)).isZero();
    }

    @Test
    void storesNothingWhenPokeApiIsDown() {
        pokeApi.stubFor(get("/pokemon/bulbasaur").willReturn(serviceUnavailable()));

        client.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idOrName\": \"bulbasaur\"}")
                .exchange()
                .expectStatus().isEqualTo(503);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pokemon", Integer.class)).isZero();
    }

    @Test
    void answersNotFoundForAnIdThatIsNotInTheCollection() {
        client.get().uri(COLLECTION + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Pokémon not in the collection");
    }

    @Test
    void editsTheProprietaryFieldsAndReadsThemBack() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");

        client.put().uri(COLLECTION + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"version": 0, "nickname": " Bulby ", "notes": "Our first starter", "region": "Kanto",
                         "localizedNames": {"pt-BR": "Bulbassauro"}, "tags": ["Starter"]}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.version").isEqualTo(1);

        client.get().uri(COLLECTION + "/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        {"nickname": "Bulby", "notes": "Our first starter", "region": "Kanto",
                         "localizedNames": {"pt-BR": "Bulbassauro"}, "tags": ["starter"], "version": 1,
                         "name": "bulbasaur", "weightKg": 6.9}
                        """);
        client.get().uri(COLLECTION + "?name=bulbass").exchange()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void refusesAnEditMadeFromAnOutdatedCopyAndKeepsTheFirstOne() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");
        edit(id, "{\"version\": 0, \"nickname\": \"Bulby\"}").expectStatus().isOk();

        edit(id, "{\"version\": 0, \"nickname\": \"Seedy\"}")
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.expectedVersion").isEqualTo(0)
                .jsonPath("$.currentVersion").isEqualTo(1);

        client.get().uri(COLLECTION + "/" + id).exchange()
                .expectBody().jsonPath("$.nickname").isEqualTo("Bulby");
    }

    @Test
    void refusesToEditDataThatComesFromPokeApi() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");

        edit(id, "{\"version\": 0, \"name\": \"bulbasaurus\"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].field").isEqualTo("name");

        client.get().uri(COLLECTION + "/" + id).exchange()
                .expectBody().jsonPath("$.name").isEqualTo("bulbasaur").jsonPath("$.version").isEqualTo(0);
    }

    @Test
    void removesAPokemonAndThenReportsItAsMissing() {
        stubBulbasaur(69);
        var id = syncAndReadId("bulbasaur");

        client.delete().uri(COLLECTION + "/" + id).exchange().expectStatus().isNoContent();

        client.get().uri(COLLECTION + "/" + id).exchange().expectStatus().isNotFound();
        client.delete().uri(COLLECTION + "/" + id).exchange().expectStatus().isNotFound();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pokemon", Integer.class)).isZero();
    }

    @Test
    void canAddAPokemonAgainAfterRemovingIt() {
        stubBulbasaur(69);
        var first = syncAndReadId("bulbasaur");
        client.delete().uri(COLLECTION + "/" + first).exchange().expectStatus().isNoContent();

        var second = syncAndReadId("bulbasaur");

        assertThat(second).isNotEqualTo(first);
    }

    private RestTestClient.ResponseSpec edit(UUID id, String body) {
        return client.put().uri(COLLECTION + "/" + id).contentType(MediaType.APPLICATION_JSON).body(body).exchange();
    }

    private UUID syncAndReadId(String idOrName) {
        var id = client.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"idOrName\": \"" + idOrName + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult().getResponseBody().get("id");
        return UUID.fromString((String) id);
    }

    /** Serves Bulbasaur; the weight can change between calls to show a re-sync picking up new source data. */
    private static void stubBulbasaur(int weightHectograms) {
        var pokemon = read("pokeapi/pokemon-bulbasaur.json").replace("\"weight\": 69", "\"weight\": " + weightHectograms);
        for (var key : new String[]{"bulbasaur", "1"}) {
            pokeApi.stubFor(get("/pokemon/" + key).willReturn(okJson(pokemon)));
        }
        pokeApi.stubFor(get("/pokemon-species/bulbasaur").willReturn(okJson(read("pokeapi/pokemon-species-bulbasaur.json"))));
        pokeApi.stubFor(get("/evolution-chain/1").willReturn(okJson(read("pokeapi/evolution-chain-1.json"))));
    }
}
