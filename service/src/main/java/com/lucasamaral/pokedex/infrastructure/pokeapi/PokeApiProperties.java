package com.lucasamaral.pokedex.infrastructure.pokeapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("pokedex.pokeapi")
public record PokeApiProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
}
