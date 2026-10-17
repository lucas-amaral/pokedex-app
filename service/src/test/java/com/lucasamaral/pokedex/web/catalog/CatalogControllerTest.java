package com.lucasamaral.pokedex.web.catalog;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.pokemon.Ability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.util.List;

import static com.lucasamaral.pokedex.support.PokemonSummaryBuilder.aPokemonSummary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    private static final String URL = "/api/v1/catalog/pokemon";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BrowseCatalogUseCase browseCatalog;

    @Test
    void returnsOnePageOfSummaries() {
        var bulbasaur = aPokemonSummary()
                .withAbilities(List.of(new Ability("overgrow", false), new Ability("chlorophyll", true)))
                .build();
        given(browseCatalog.browse(new PageQuery(0, 2)))
                .willReturn(new Page<>(List.of(bulbasaur), new PageQuery(0, 2), 1351));

        var response = mvc.get().uri(URL).param("page", "0").param("size", "2");

        assertThat(response).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {
                  "content": [{
                    "id": 1,
                    "name": "bulbasaur",
                    "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                    "category": "Seed Pokémon",
                    "weightKg": 6.9,
                    "abilities": [
                      {"name": "overgrow", "hidden": false},
                      {"name": "chlorophyll", "hidden": true}
                    ]
                  }],
                  "page": 0,
                  "size": 2,
                  "totalElements": 1351,
                  "totalPages": 676,
                  "hasNext": true
                }
                """);
    }

    @Test
    void defaultsToTheFirstPageOfTwenty() {
        given(browseCatalog.browse(any())).willReturn(new Page<>(List.of(), PageQuery.first(20), 0));

        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.OK);

        verify(browseCatalog).browse(new PageQuery(0, 20));
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 51"})
    void rejectsOutOfRangePagination(String page, String size) {
        var response = mvc.get().uri(URL).param("page", page).param("size", size);

        assertThat(response)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(400);
        verifyNoInteractions(browseCatalog);
    }

    @Test
    void explainsWhichParameterIsInvalidInEnglishWhateverTheClientLanguage() {
        var response = mvc.get().uri(URL).param("size", "51").header("Accept-Language", "pt-BR");

        assertThat(response).bodyJson().extractingPath("$.errors[0].parameter").isEqualTo("size");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo("must be less than or equal to 50");
    }

    @Test
    void rejectsNonNumericPage() {
        var response = mvc.get().uri(URL).param("page", "abc");

        assertThat(response)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        verify(browseCatalog, never()).browse(any());
    }

    @Test
    void answersServiceUnavailableWhenPokeApiIsDown() {
        given(browseCatalog.browse(any()))
                .willThrow(new CatalogUnavailableException("PokeAPI request failed: 500", new IOException("boom")));

        var response = mvc.get().uri(URL);

        assertThat(response)
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Catalog unavailable");
        assertThat(response).bodyJson().extractingPath("$.detail").asString().doesNotContain("boom", "500");
    }
}
