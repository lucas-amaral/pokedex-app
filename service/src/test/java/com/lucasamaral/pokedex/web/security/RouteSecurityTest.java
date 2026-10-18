package com.lucasamaral.pokedex.web.security;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
import com.lucasamaral.pokedex.application.catalog.GetPokemonDetailsUseCase;
import com.lucasamaral.pokedex.application.collection.BrowseCollectionUseCase;
import com.lucasamaral.pokedex.application.collection.RemoveCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.collection.SyncPokemonUseCase;
import com.lucasamaral.pokedex.application.collection.SyncResult;
import com.lucasamaral.pokedex.application.collection.UpdateCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.application.user.AuthenticateUserUseCase;
import com.lucasamaral.pokedex.application.user.GetCurrentUserUseCase;
import com.lucasamaral.pokedex.application.user.RegisterUserUseCase;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Who may call what: reads are public, changes need a login, removing a Pokémon needs ADMIN.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class RouteSecurityTest {

    private static final String SYNC_BODY = "{\"idOrName\": \"bulbasaur\"}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BrowseCatalogUseCase browseCatalog;
    @MockitoBean
    private GetPokemonDetailsUseCase getPokemonDetails;
    @MockitoBean
    private SyncPokemonUseCase syncPokemon;
    @MockitoBean
    private BrowseCollectionUseCase browseCollection;
    @MockitoBean
    private UpdateCollectedPokemonUseCase updatePokemon;
    @MockitoBean
    private RemoveCollectedPokemonUseCase removePokemon;
    @MockitoBean
    private RegisterUserUseCase register;
    @MockitoBean
    private AuthenticateUserUseCase authenticate;
    @MockitoBean
    private GetCurrentUserUseCase currentUser;

    private final CollectedPokemon bulbasaur = CollectedPokemon.collect(aPokemonDetails().build(), Instant.now());

    @Test
    void anyoneCanBrowseTheCatalogAndTheCollection() {
        given(browseCatalog.browse(any())).willReturn(new Page<>(List.of(), PageQuery.first(20), 0));
        given(browseCollection.list(any(), any())).willReturn(new Page<>(List.of(), PageQuery.first(20), 0));

        assertThat(mvc.get().uri("/api/v1/catalog/pokemon")).hasStatus(HttpStatus.OK);
        assertThat(mvc.get().uri("/api/v1/pokemon")).hasStatus(HttpStatus.OK);
    }

    @Test
    void addingToTheCollectionNeedsALogin() {
        var response = mvc.post().uri("/api/v1/pokemon/sync").contentType(MediaType.APPLICATION_JSON).content(SYNC_BODY);

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).hasHeader("WWW-Authenticate", "Bearer");
    }

    @Test
    void aLoggedInUserCanAddToTheCollection() {
        given(syncPokemon.sync("bulbasaur")).willReturn(new SyncResult(bulbasaur, true));

        var response = mvc.post().uri("/api/v1/pokemon/sync").with(user())
                .contentType(MediaType.APPLICATION_JSON).content(SYNC_BODY);

        assertThat(response).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void editingNeedsALogin() {
        var response = mvc.put().uri("/api/v1/pokemon/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\": 0}");

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aRegularUserCannotRemoveAPokemon() {
        var response = mvc.delete().uri("/api/v1/pokemon/{id}", UUID.randomUUID()).with(user());

        assertThat(response).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not allowed");
    }

    @Test
    void anAdminCanRemoveAPokemon() {
        assertThat(mvc.delete().uri("/api/v1/pokemon/{id}", UUID.randomUUID()).with(admin()))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void anInvalidTokenIsRefused() {
        var response = mvc.post().uri("/api/v1/pokemon/sync").header("Authorization", "Bearer not-a-real-token")
                .contentType(MediaType.APPLICATION_JSON).content(SYNC_BODY);

        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void anyoneCanRegisterAndLogIn() {
        var register = mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}");
        var login = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}");

        assertThat(register).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(login).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
