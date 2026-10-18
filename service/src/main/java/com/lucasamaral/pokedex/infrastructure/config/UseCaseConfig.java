package com.lucasamaral.pokedex.infrastructure.config;

import com.lucasamaral.pokedex.application.catalog.BrowseCatalogService;
import com.lucasamaral.pokedex.application.catalog.BrowseCatalogUseCase;
import com.lucasamaral.pokedex.application.catalog.GetPokemonDetailsService;
import com.lucasamaral.pokedex.application.catalog.GetPokemonDetailsUseCase;
import com.lucasamaral.pokedex.application.catalog.PokemonCatalog;
import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.collection.BrowseCollectionService;
import com.lucasamaral.pokedex.application.collection.BrowseCollectionUseCase;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.EditCollectionService;
import com.lucasamaral.pokedex.application.collection.SyncPokemonService;
import com.lucasamaral.pokedex.application.collection.SyncPokemonUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the framework-free use cases into Spring. The application layer itself has no Spring annotations.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    BrowseCatalogUseCase browseCatalogUseCase(PokemonCatalog pokemonCatalog) {
        return new BrowseCatalogService(pokemonCatalog);
    }

    @Bean
    GetPokemonDetailsUseCase getPokemonDetailsUseCase(PokemonDetailsCatalog pokemonDetailsCatalog) {
        return new GetPokemonDetailsService(pokemonDetailsCatalog);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SyncPokemonUseCase syncPokemonUseCase(PokemonDetailsCatalog pokemonDetailsCatalog, CollectionRepository collectionRepository,
                                          Clock clock) {
        return new SyncPokemonService(pokemonDetailsCatalog, collectionRepository, clock);
    }

    @Bean
    BrowseCollectionUseCase browseCollectionUseCase(CollectionRepository collectionRepository) {
        return new BrowseCollectionService(collectionRepository);
    }

    @Bean
    EditCollectionService editCollectionService(CollectionRepository collectionRepository) {
        return new EditCollectionService(collectionRepository);
    }
}
