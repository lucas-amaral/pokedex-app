package com.lucasamaral.pokedex.web.collection;

import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.application.collection.BrowseCollectionUseCase;
import com.lucasamaral.pokedex.application.collection.CollectedPokemonNotFoundException;
import com.lucasamaral.pokedex.application.collection.CollectedPokemonVersionConflictException;
import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.RemoveCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.collection.SyncPokemonUseCase;
import com.lucasamaral.pokedex.application.collection.SyncResult;
import com.lucasamaral.pokedex.application.collection.UpdateCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.collection.CustomFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

    private static final String COLLECTION = "/api/v1/pokemon";
    private static final Instant SYNCED = Instant.parse("2026-10-18T10:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SyncPokemonUseCase syncPokemon;

    @MockitoBean
    private BrowseCollectionUseCase browseCollection;

    @MockitoBean
    private UpdateCollectedPokemonUseCase updatePokemon;

    @MockitoBean
    private RemoveCollectedPokemonUseCase removePokemon;

    private final CollectedPokemon bulbasaur = CollectedPokemon.collect(aPokemonDetails().build(), SYNCED)
            .withCustomFields(new CustomFields("Bulby", "Our first starter", "Kanto",
                    Map.of("pt-BR", "Bulbassauro"), Set.of("starter")));

    @Test
    void addsANewPokemonAndPointsToIt() {
        given(syncPokemon.sync("bulbasaur")).willReturn(new SyncResult(bulbasaur, true));

        var response = mvc.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idOrName\": \"bulbasaur\"}");

        assertThat(response).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/api/v1/pokemon/" + bulbasaur.id());
        assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(bulbasaur.id().toString());
        assertThat(response).bodyJson().extractingPath("$.pokeapiId").isEqualTo(1);
    }

    @Test
    void answersOkWhenThePokemonWasAlreadyInTheCollection() {
        given(syncPokemon.sync("bulbasaur")).willReturn(new SyncResult(bulbasaur, false));

        var response = mvc.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idOrName\": \"bulbasaur\"}");

        assertThat(response).hasStatus(HttpStatus.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"idOrName\": \"\"}", "{\"idOrName\": \"mew!\"}", "{}"})
    void rejectsAMissingOrMalformedNumberOrName(String body) {
        var response = mvc.post().uri(COLLECTION + "/sync").contentType(MediaType.APPLICATION_JSON).content(body);

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("idOrName");
        verifyNoInteractions(syncPokemon);
    }

    @Test
    void rejectsABodyThatIsNotJson() {
        var response = mvc.post().uri(COLLECTION + "/sync").contentType(MediaType.APPLICATION_JSON).content("bulbasaur")
                .exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("The request body is not valid JSON.");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
            "{\"version\": \"abc\"}                     | version        | must be a number",
            "{\"version\": 0, \"tags\": \"starter\"}      | tags           | must be a list",
            "{\"version\": 0, \"localizedNames\": []}  | localizedNames | must be an object",
            "{\"version\": 0, \"nickname\": [\"Bulby\"]} | nickname       | must be text"})
    void namesTheFieldThatHasTheWrongType(String body, String field, String message) {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo(message);
        verifyNoInteractions(updatePokemon);
    }

    @Test
    void answersNotFoundWhenPokeApiDoesNotKnowThePokemon() {
        given(syncPokemon.sync("missingno")).willThrow(new PokemonNotFoundException("missingno"));

        var response = mvc.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idOrName\": \"missingno\"}");

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Pokémon not found");
    }

    @Test
    void answersServiceUnavailableWhenPokeApiIsDown() {
        given(syncPokemon.sync(any())).willThrow(new CatalogUnavailableException("down", new IOException("boom")));

        var response = mvc.post().uri(COLLECTION + "/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idOrName\": \"bulbasaur\"}");

        assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void listsTheCollectionWithItsProprietaryFields() {
        given(browseCollection.list(CollectionFilter.none(), new PageQuery(0, 20)))
                .willReturn(new Page<>(List.of(bulbasaur), PageQuery.first(20), 1));

        var response = mvc.get().uri(COLLECTION);

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {
                  "content": [{
                    "id": "%s",
                    "pokeapiId": 1,
                    "name": "bulbasaur",
                    "nickname": "Bulby",
                    "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                    "category": "Seed Pokémon",
                    "types": ["grass", "poison"],
                    "tags": ["starter"],
                    "syncedAt": "2026-10-18T10:00:00Z"
                  }],
                  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false
                }
                """.formatted(bulbasaur.id()));
    }

    @Test
    void passesTheFiltersToTheUseCase() {
        given(browseCollection.list(any(), any())).willReturn(new Page<>(List.of(), PageQuery.first(10), 0));

        assertThat(mvc.get().uri(COLLECTION + "?page=0&size=10&name=bulb&tag=starter")).hasStatus(HttpStatus.OK);

        verify(browseCollection).list(new CollectionFilter("bulb", "starter"), new PageQuery(0, 10));
    }

    @Test
    void passesThePokeApiNumberFilterToTheUseCase() {
        given(browseCollection.list(any(), any())).willReturn(new Page<>(List.of(), PageQuery.first(1), 0));

        assertThat(mvc.get().uri(COLLECTION + "?size=1&pokeapiId=25")).hasStatus(HttpStatus.OK);

        verify(browseCollection).list(new CollectionFilter(null, null, 25), new PageQuery(0, 1));
    }

    @Test
    void rejectsANonPositivePokeApiNumber() {
        assertThat(mvc.get().uri(COLLECTION + "?pokeapiId=0")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(browseCollection);
    }

    @Test
    void rejectsAPageSizeAboveTheLimit() {
        assertThat(mvc.get().uri(COLLECTION + "?size=51")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(browseCollection);
    }

    @Test
    void returnsOneCollectedPokemonWithEverything() {
        given(browseCollection.get(bulbasaur.id())).willReturn(bulbasaur);

        var response = mvc.get().uri(COLLECTION + "/{id}", bulbasaur.id());

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {
                  "id": "%s",
                  "pokeapiId": 1,
                  "name": "bulbasaur",
                  "speciesName": "bulbasaur",
                  "category": "Seed Pokémon",
                  "heightM": 0.7,
                  "weightKg": 6.9,
                  "types": ["grass", "poison"],
                  "abilities": [{"name": "overgrow", "hidden": false}, {"name": "chlorophyll", "hidden": true}],
                  "stats": [
                    {"name": "hp", "value": 45}, {"name": "attack", "value": 49}, {"name": "defense", "value": 49},
                    {"name": "special-attack", "value": 65}, {"name": "special-defense", "value": 65}, {"name": "speed", "value": 45}
                  ],
                  "nickname": "Bulby",
                  "notes": "Our first starter",
                  "region": "Kanto",
                  "localizedNames": {"pt-BR": "Bulbassauro"},
                  "tags": ["starter"],
                  "syncedAt": "2026-10-18T10:00:00Z",
                  "version": 0
                }
                """.formatted(bulbasaur.id()));
    }

    @Test
    void answersNotFoundForAPokemonThatIsNotInTheCollection() {
        var unknown = UUID.randomUUID();
        given(browseCollection.get(unknown)).willThrow(new CollectedPokemonNotFoundException(unknown));

        var response = mvc.get().uri(COLLECTION + "/{id}", unknown);

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Pokémon not in the collection");
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() {
        assertThat(mvc.get().uri(COLLECTION + "/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(browseCollection);
    }

    private static final String EDIT = """
            {
              "version": 3,
              "nickname": " Bulby ",
              "notes": "Our first starter",
              "region": "Kanto",
              "localizedNames": {"pt-BR": "Bulbassauro"},
              "tags": ["Starter"]
            }
            """;

    @Test
    void replacesTheProprietaryFields() {
        var edited = new CustomFields("Bulby", "Our first starter", "Kanto", Map.of("pt-BR", "Bulbassauro"), Set.of("starter"));
        given(updatePokemon.update(bulbasaur.id(), 3, edited)).willReturn(bulbasaur.withCustomFields(edited));

        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id()).contentType(MediaType.APPLICATION_JSON).content(EDIT)
                .exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.nickname").isEqualTo("Bulby");
        assertThat(response).bodyJson().extractingPath("$.tags[0]").isEqualTo("starter");
        verify(updatePokemon).update(bulbasaur.id(), 3, edited);
    }

    @Test
    void clearsTheFieldsLeftOut() {
        given(updatePokemon.update(any(), anyLong(), any())).willReturn(bulbasaur.withCustomFields(CustomFields.empty()));

        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\": 0}");

        assertThat(response).hasStatus(HttpStatus.OK);
        verify(updatePokemon).update(bulbasaur.id(), 0, CustomFields.empty());
    }

    @Test
    void requiresTheVersionTheEditStartedFrom() {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\": \"Bulby\"}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("version");
        verifyNoInteractions(updatePokemon);
    }

    @Test
    void rejectsANicknameThatIsTooLong() {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"nickname\": \"%s\"}".formatted("n".repeat(31)));

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("nickname");
        verifyNoInteractions(updatePokemon);
    }

    @Test
    void rejectsAMalformedTag() {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"tags\": [\"has space\"]}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("tags");
        verifyNoInteractions(updatePokemon);
    }

    @Test
    void rejectsAMalformedLanguageTag() {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"localizedNames\": {\"pt_BR\": \"Bulbassauro\"}}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("localizedNames");
    }

    @Test
    void refusesToChangeDataThatComesFromPokeApi() {
        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"name\": \"bulbasaurus\"}");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").asString().contains("not an editable field");
        verifyNoInteractions(updatePokemon);
    }

    @Test
    void answersConflictWhenSomeoneElseChangedThePokemonFirst() {
        given(updatePokemon.update(eq(bulbasaur.id()), eq(3L), any()))
                .willThrow(new CollectedPokemonVersionConflictException(bulbasaur.id(), 3, 4));

        var response = mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id()).contentType(MediaType.APPLICATION_JSON).content(EDIT);

        assertThat(response).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Pokémon changed since you loaded it");
        assertThat(response).bodyJson().extractingPath("$.currentVersion").isEqualTo(4);
    }

    @Test
    void answersNotFoundWhenEditingAPokemonThatIsNotInTheCollection() {
        given(updatePokemon.update(any(), anyLong(), any())).willThrow(new CollectedPokemonNotFoundException(bulbasaur.id()));

        assertThat(mvc.put().uri(COLLECTION + "/{id}", bulbasaur.id()).contentType(MediaType.APPLICATION_JSON).content(EDIT))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void removesAPokemonFromTheCollection() {
        assertThat(mvc.delete().uri(COLLECTION + "/{id}", bulbasaur.id())).hasStatus(HttpStatus.NO_CONTENT);

        verify(removePokemon).remove(bulbasaur.id());
    }

    @Test
    void answersNotFoundWhenRemovingAPokemonThatIsNotInTheCollection() {
        willThrow(new CollectedPokemonNotFoundException(bulbasaur.id())).given(removePokemon).remove(bulbasaur.id());

        assertThat(mvc.delete().uri(COLLECTION + "/{id}", bulbasaur.id())).hasStatus(HttpStatus.NOT_FOUND);
    }
}
