package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.lucasamaral.pokedex.application.catalog.PokemonCatalog;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfig {

    @Bean
    PokeApiClient pokeApiClient(RestClient.Builder builder, PokeApiProperties properties) {
        return createClient(builder, properties);
    }

    @Bean
    PokemonCatalog pokemonCatalog(PokeApiClient client, AsyncTaskExecutor applicationTaskExecutor) {
        return new PokeApiCatalogAdapter(client, applicationTaskExecutor);
    }

    static PokeApiClient createClient(RestClient.Builder builder, PokeApiProperties properties) {
        var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        var restClient = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(PokeApiClient.class);
    }
}
