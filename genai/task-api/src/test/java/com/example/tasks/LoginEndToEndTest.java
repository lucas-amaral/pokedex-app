package com.example.tasks;

import com.example.tasks.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** The real stack: HTTP, the use case, BCrypt and the demo user stored by Flyway in PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
@AutoConfigureRestTestClient
class LoginEndToEndTest {

    @Autowired
    private RestTestClient client;

    @Test
    void aDemoUserLogsInWithThePublishedPassword() {
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": \"alice\", \"password\": \"wonderland123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.accessToken").isNotEmpty();
    }

    @Test
    void aWrongPasswordIsRefused() {
        client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": \"alice\", \"password\": \"not-her-password\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
