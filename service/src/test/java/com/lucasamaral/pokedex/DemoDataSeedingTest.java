package com.lucasamaral.pokedex;

import com.lucasamaral.pokedex.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;


/**
 * Seeding at startup, through the whole stack: the snapshot goes into PostgreSQL through the sync use case, and the
 * demo accounts can log in. PokeAPI is never called.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "pokedex.seed.enabled=true",
        "pokedex.seed.snapshot=seed/test-snapshot.json",
        "pokedex.pokeapi.base-url=http://localhost:1"})
@Import(PostgresTestConfiguration.class)
@AutoConfigureRestTestClient
class DemoDataSeedingTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private ApplicationRunner demoDataSeeding;

    @Test
    void fillsTheCollectionFromTheSnapshotWithItsDemoFields() {
        client.get().uri("/api/v1/pokemon?name=bulbasaur").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].name").isEqualTo("bulbasaur")
                .jsonPath("$.content[0].tags").isEqualTo(List.of("gen-1", "starter"));
        client.get().uri("/api/v1/pokemon").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void createsAnAdminAndARegularDemoAccount() {
        logIn("oak", "professor123").jsonPath("$.user.role").isEqualTo("ADMIN");
        logIn("ash", "pikachu123").jsonPath("$.user.role").isEqualTo("USER");
    }

    @Test
    void doesNothingTheSecondTime() throws Exception {
        demoDataSeeding.run(new DefaultApplicationArguments());

        client.get().uri("/api/v1/pokemon").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);
    }

    private RestTestClient.BodyContentSpec logIn(String username, String password) {
        return client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }
}
