package com.example.tasks;

import com.example.tasks.config.JwtProperties;
import com.example.tasks.config.SecurityConfig;
import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * The whole stack over real HTTP: the demo users log in through the API, and every endpoint runs against PostgreSQL
 * with the real migrations. Dates are relative to today, so the test never goes stale.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
@AutoConfigureRestTestClient
class TaskLifecycleEndToEndTest {

    private static final String TASKS = "/api/v1/tasks";

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtProperties jwtProperties;

    private String alice;
    private String bob;

    @BeforeEach
    void logInBothDemoUsers() {
        jdbc.execute("DELETE FROM tasks");
        alice = logIn("alice", "wonderland123");
        bob = logIn("bob", "builder12345");
    }

    @Test
    void aTaskFromCreationToDeletion() {
        var tomorrow = LocalDate.now().plusDays(1);
        var location = client.post().uri(TASKS).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\": \"Buy milk\", \"description\": \"2 liters\", \"dueDate\": \"%s\"}".formatted(tomorrow))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.status").isEqualTo("TODO")
                .returnResult().getResponseHeaders().getLocation();

        client.get().uri(location).header(HttpHeaders.AUTHORIZATION, alice).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Buy milk").jsonPath("$.version").isEqualTo(0);

        client.get().uri(TASKS + "?status=TODO").header(HttpHeaders.AUTHORIZATION, alice).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);

        client.put().uri(location).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"version\": 0, \"title\": \"Buy oat milk\", \"dueDate\": \"%s\"}".formatted(tomorrow))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.version").isEqualTo(1).jsonPath("$.description").doesNotExist();

        client.put().uri(location).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"version\": 0, \"title\": \"From a stale copy\"}")
                .exchange()
                .expectStatus().isEqualTo(409);

        client.patch().uri(location + "/status").header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"version\": 1, \"status\": \"DONE\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("DONE").jsonPath("$.version").isEqualTo(2);

        client.patch().uri(location + "/status").header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"version\": 2, \"status\": \"TODO\"}")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.allowed[0]").isEqualTo("IN_PROGRESS");

        client.delete().uri(location + "?version=2").header(HttpHeaders.AUTHORIZATION, alice).exchange()
                .expectStatus().isNoContent();

        client.get().uri(location).header(HttpHeaders.AUTHORIZATION, alice).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void anotherUserCanNeitherSeeNorChangeTheTask() {
        var location = client.post().uri(TASKS).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\": \"Alice's secret\"}")
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class).getResponseHeaders().getLocation();

        client.get().uri(location).header(HttpHeaders.AUTHORIZATION, bob).exchange().expectStatus().isNotFound();
        client.delete().uri(location + "?version=0").header(HttpHeaders.AUTHORIZATION, bob).exchange().expectStatus().isNotFound();
        client.get().uri(TASKS).header(HttpHeaders.AUTHORIZATION, bob).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(0);
        client.get().uri(location).header(HttpHeaders.AUTHORIZATION, alice).exchange().expectStatus().isOk();
    }

    @Test
    void nothingWithoutAToken() {
        client.get().uri(TASKS).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    /** Found by sending hostile requests to the running stack: a correctly signed token for an account that no longer exists gave a 500. */
    @Test
    void refusesATokenWhoseAccountNoLongerExists() {
        var deletedUser = signedToken(UUID.randomUUID().toString());

        client.get().uri(TASKS).header(HttpHeaders.AUTHORIZATION, deletedUser).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        client.post().uri(TASKS).header(HttpHeaders.AUTHORIZATION, deletedUser).contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\": \"Ghost\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /** Found by sending hostile requests to the running stack: a correctly signed token whose subject is not a user id gave a 500. */
    @Test
    void refusesATokenWhoseSubjectIsNotAUserId() {
        client.get().uri(TASKS).header(HttpHeaders.AUTHORIZATION, signedToken("admin")).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void documentsTheApiWithTheBearerScheme() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("Task API")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
                .jsonPath("$.paths['/api/v1/tasks/{id}/status'].patch").exists();
    }

    /** Signed with the application's own key, as only a leaked key or a bug in the issuer could produce. */
    private String signedToken(String subject) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).subject(subject).issuedAt(now)
                .expiresAt(now.plusSeconds(600)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + SecurityConfig.createJwtEncoder(jwtProperties).encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @SuppressWarnings("unchecked")
    private String logIn(String username, String password) {
        var body = client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        return "Bearer " + body.get("accessToken");
    }
}
