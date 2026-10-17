package com.lucasamaral.pokedex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class PokedexServiceApplicationTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void reportsHealthUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void publishesOpenApiDocumentWithApiTitle() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.info.title").isEqualTo("Pokedex API");
    }

    @Test
    void servesSwaggerUi() {
        assertThat(mvc.get().uri("/swagger-ui/index.html"))
                .hasStatus(HttpStatus.OK);
    }
}
