package com.example.tasks.controller;

import com.example.tasks.config.JsonConfig;
import com.example.tasks.config.SecurityConfig;
import com.example.tasks.repository.UserRepository;
import com.example.tasks.service.AccessToken;
import com.example.tasks.service.InvalidCredentialsException;
import com.example.tasks.service.LogInUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JsonConfig.class})
class AuthControllerTest {

    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired
    private MockMvcTester mvc;

    /** The security chain checks that a token's account still exists; tests here authenticate with jwt() instead. */
    @MockitoBean
    private UserRepository users;

    @MockitoBean
    private LogInUseCase logIn;

    @Test
    void returnsABearerTokenWithItsExpiry() {
        given(logIn.logIn("alice", "wonderland123"))
                .willReturn(new AccessToken("signed.jwt.value", Instant.parse("2030-01-01T00:00:00Z")));

        var response = mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"alice\", \"password\": \"wonderland123\"}");

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().isStrictlyEqualTo("""
                {"accessToken": "signed.jwt.value", "tokenType": "Bearer", "expiresAt": "2030-01-01T00:00:00Z"}
                """);
    }

    @Test
    void answersWrongCredentialsWithAProblemThatDoesNotSayWhichPartWasWrong() {
        given(logIn.logIn("alice", "nope-nope")).willThrow(new InvalidCredentialsException());

        var response = mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"alice\", \"password\": \"nope-nope\"}").exchange();

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Login failed");
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("Wrong username or password.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"password\": \"wonderland123\"}                              | username | must not be blank",
            "{\"username\": \"alice\", \"password\": \"\"}                  | password | must not be blank",
            "{\"username\": \"alice\", \"password\": 123456789}             | password | must be text",
            "{\"username\": \"alice\", \"password\": \"x12345678\", \"admin\": true} | admin | is not a known field"})
    void namesTheFieldThatIsWrong(String body, String field, String message) {
        var response = mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(logIn);
    }

    @Test
    void refusesABodyThatIsNotJson() {
        var response = mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON).content("alice:wonderland123").exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("The request body is not valid JSON.");
    }

    @Test
    void protectsEverythingElseWithAProblemAndABearerChallenge() {
        var response = mvc.get().uri("/api/v1/tasks").exchange();

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).hasHeader("WWW-Authenticate", "Bearer");
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Authentication required");
    }

    @Test
    void refusesAnInvalidToken() {
        var response = mvc.get().uri("/api/v1/tasks").header("Authorization", "Bearer not-a-jwt").exchange();

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
