package com.lucasamaral.pokedex.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI pokedexOpenApi(@Value("${pokedex.api.version}") String version) {
        return new OpenAPI().info(new Info()
                .title("Pokedex API")
                .version(version)
                .description("Browse the PokeAPI catalog, keep a local Pokémon collection with custom fields, and manage users."));
    }
}
