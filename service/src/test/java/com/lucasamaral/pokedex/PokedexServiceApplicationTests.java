package com.lucasamaral.pokedex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class PokedexServiceApplicationTests {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AsyncTaskExecutor applicationTaskExecutor;

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

    @Test
    void runsBackgroundWorkOnVirtualThreads() throws ExecutionException, InterruptedException {
        var runsOnVirtualThread = applicationTaskExecutor.submit(() -> Thread.currentThread().isVirtual()).get();

        assertThat(runsOnVirtualThread).isTrue();
    }

    @Test
    void documentsTheErrorResponsesOfTheCatalog() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.paths['/api/v1/catalog/pokemon'].get.responses")
                .asMap().containsKeys("200", "400", "503");
    }
}
