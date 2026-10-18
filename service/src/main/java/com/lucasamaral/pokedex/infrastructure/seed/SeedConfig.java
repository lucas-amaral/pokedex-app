package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.SyncPokemonService;
import com.lucasamaral.pokedex.application.collection.UpdateCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.seed.DemoDataSeeder;
import com.lucasamaral.pokedex.application.user.PasswordHasher;
import com.lucasamaral.pokedex.application.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * Loads the demo data once the application has started: Generations 1 and 2 from the bundled snapshot, and the demo
 * accounts. Off unless {@code pokedex.seed.enabled} is true.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeedProperties.class)
@ConditionalOnBooleanProperty("pokedex.seed.enabled")
class SeedConfig {

    private static final Logger log = LoggerFactory.getLogger(SeedConfig.class);

    @Bean
    ApplicationRunner demoDataSeeding(SeedProperties properties, CollectionRepository collection,
                                      UpdateCollectedPokemonUseCase update, UserRepository users,
                                      PasswordHasher passwordHasher, Clock clock, JsonMapper json) {
        return arguments -> {
            var snapshot = SnapshotPokemonCatalog.load(new ClassPathResource(properties.snapshot()), json);
            var fromSnapshot = new SyncPokemonService(snapshot, collection, clock);
            var seeder = new DemoDataSeeder(fromSnapshot, update, collection, users, passwordHasher, clock);
            var report = seeder.seed(properties.demoAccounts(), snapshot.keys());
            log.info("Demo data: {} account(s) created, {} Pokémon added to the collection",
                    report.accountsCreated(), report.pokemonAdded());
        };
    }
}
