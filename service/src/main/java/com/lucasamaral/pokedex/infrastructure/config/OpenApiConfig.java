package com.lucasamaral.pokedex.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI pokedexOpenApi(@Value("${pokedex.api.version}") String version) {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Log in with POST /api/v1/auth/login and paste the accessToken here")))
                .info(new Info()
                .title("Pokedex API")
                .version(version)
                .description("Browse the PokeAPI catalog, keep a local Pokémon collection with custom fields, and manage users."));
    }
}
