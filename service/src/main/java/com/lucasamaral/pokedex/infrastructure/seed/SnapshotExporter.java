package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.stream.IntStream;

/**
 * Writes the seed snapshot from live PokeAPI, then stops the application. Run only by hand, with the
 * {@code export-snapshot} profile (see the README); the snapshot is committed so seeding never needs the network.
 */
@Configuration(proxyBeanMethods = false)
@Profile("export-snapshot")
class SnapshotExporter {

    private static final Logger log = LoggerFactory.getLogger(SnapshotExporter.class);
    private static final int LAST_OF_GENERATION_2 = 251;

    @Bean
    ApplicationRunner exportSnapshot(PokemonDetailsCatalog pokeApi, JsonMapper json, Clock clock,
                                     ConfigurableApplicationContext context,
                                     @Value("${pokedex.pokeapi.base-url}") String source,
                                     @Value("${pokedex.seed.export-to:src/main/resources/" + SeedProperties.DEFAULT_SNAPSHOT + "}") Path target) {
        return arguments -> {
            var pokemon = IntStream.rangeClosed(1, LAST_OF_GENERATION_2)
                    .mapToObj(id -> pokeApi.findDetails(String.valueOf(id))
                            .orElseThrow(() -> new IllegalStateException("PokeAPI has no Pokémon " + id)))
                    .map(SnapshotFile.Entry::from)
                    .toList();
            var file = new SnapshotFile(source, clock.instant(), pokemon);
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.writeString(target, json.rebuild().enable(SerializationFeature.INDENT_OUTPUT).build().writeValueAsString(file) + "\n");
            log.info("Wrote {} Pokémon to {}", pokemon.size(), target.toAbsolutePath());
            System.exit(SpringApplication.exit(context, () -> 0));
        };
    }
}
