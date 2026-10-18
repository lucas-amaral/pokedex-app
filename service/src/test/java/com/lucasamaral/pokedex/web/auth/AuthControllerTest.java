package com.lucasamaral.pokedex.web.auth;

import com.lucasamaral.pokedex.application.user.AccessToken;
import com.lucasamaral.pokedex.application.user.AuthenticateUserUseCase;
import com.lucasamaral.pokedex.application.user.AuthenticatedSession;
import com.lucasamaral.pokedex.application.user.GetCurrentUserUseCase;
import com.lucasamaral.pokedex.application.user.InvalidCredentialsException;
import com.lucasamaral.pokedex.application.user.RegisterUserUseCase;
import com.lucasamaral.pokedex.application.user.Registration;
import com.lucasamaral.pokedex.application.user.EmailTakenException;
import com.lucasamaral.pokedex.application.user.UsernameTakenException;
import com.lucasamaral.pokedex.domain.InvalidFieldException;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.Role;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;
import com.lucasamaral.pokedex.infrastructure.config.JsonConfig;
import com.lucasamaral.pokedex.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JsonConfig.class})
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegisterUserUseCase register;

    @MockitoBean
    private AuthenticateUserUseCase authenticate;

    @MockitoBean
    private GetCurrentUserUseCase currentUser;

    private final User ash = new User(UUID.randomUUID(), Username.of("ash"), Email.of("ash@pallet.town"),
            "$2a$10$secret-hash", Role.USER, NOW);

    @Test
    void registersAnAccountWithoutEchoingThePassword() {
        given(register.register(new Registration("ash", "ash@pallet.town", "pikachu123"))).willReturn(ash);

        var response = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ash\", \"email\": \"ash@pallet.town\", \"password\": \"pikachu123\"}")
                .exchange();

        assertThat(response).hasStatus(HttpStatus.CREATED);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {"id": "%s", "username": "ash", "email": "ash@pallet.town", "role": "USER"}
                """.formatted(ash.id()));
        assertThat(response).bodyText().doesNotContain("pikachu123", "secret-hash");
    }

    @Test
    void explainsAnInvalidRegistrationByField() {
        given(register.register(any())).willThrow(new InvalidFieldException("password", "must be 8 to 72 characters"));

        var response = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ash\", \"email\": \"ash@pallet.town\", \"password\": \"short\"}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
    }

    @Test
    void requiresEveryRegistrationField() {
        var response = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors.length()").isEqualTo(3);
    }

    @Test
    void answersConflictForAUsernameThatIsTaken() {
        given(register.register(any())).willThrow(new UsernameTakenException());

        var response = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ash\", \"email\": \"ash@pallet.town\", \"password\": \"pikachu123\"}");

        assertThat(response).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Username taken");
    }

    @Test
    void answersConflictForAnEmailThatIsTaken() {
        given(register.register(any())).willThrow(new EmailTakenException());

        var response = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"misty\", \"email\": \"ash@pallet.town\", \"password\": \"pikachu123\"}");

        assertThat(response).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Email taken");
    }

    @Test
    void keepsPasswordsOutOfLogs() {
        assertThat(new LoginRequest("ash", "pikachu123").toString()).contains("ash").doesNotContain("pikachu123");
        assertThat(new RegisterRequest("ash", "ash@pallet.town", "pikachu123").toString()).doesNotContain("pikachu123");
    }

    @Test
    void logsInAndReturnsABearerToken() {
        given(authenticate.authenticate("ash", "pikachu123"))
                .willReturn(new AuthenticatedSession(ash, new AccessToken("signed.jwt.value", NOW.plusSeconds(7200))));

        var response = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ash\", \"password\": \"pikachu123\"}");

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {"accessToken": "signed.jwt.value", "tokenType": "Bearer", "expiresAt": "2026-10-18T12:00:00Z",
                 "user": {"username": "ash", "role": "USER"}}
                """);
    }

    @Test
    void refusesWrongCredentialsWith401() {
        given(authenticate.authenticate(any(), any())).willThrow(new InvalidCredentialsException());

        var response = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ash\", \"password\": \"wrong-password\"}");

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("Wrong username or password");
    }

    @Test
    void tellsWhoIsLoggedIn() {
        given(currentUser.currentUser(ash.id())).willReturn(ash);

        var response = mvc.get().uri("/api/v1/auth/me").with(jwt().jwt(token -> token.subject(ash.id().toString())));

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.username").isEqualTo("ash");
    }

    @Test
    void asksForAuthenticationWhenNobodyIsLoggedIn() {
        var response = mvc.get().uri("/api/v1/auth/me");

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Authentication required");
    }
}
