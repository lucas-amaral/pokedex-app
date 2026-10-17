package com.lucasamaral.pokedex.infrastructure.pokeapi;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.domain.pokemon.Ability;
import com.lucasamaral.pokedex.domain.pokemon.BaseStat;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionCondition;
import com.lucasamaral.pokedex.domain.pokemon.EvolutionNode;
import com.lucasamaral.pokedex.domain.pokemon.Height;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.pokemon.Weight;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.lucasamaral.pokedex.support.Fixtures.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@WireMockTest
class PokeApiCatalogAdapterDetailsTest {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    private ExecutorService executor;
    private PokeApiCatalogAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) {
        var properties = new PokeApiProperties(URI.create(wireMock.getHttpBaseUrl()), Duration.ofSeconds(1), Duration.ofSeconds(1));
        executor = Executors.newVirtualThreadPerTaskExecutor();
        adapter = new PokeApiCatalogAdapter(PokeApiConfig.createClient(RestClient.builder(), properties), executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void mapsTheDetailViewOfBulbasaur() {
        stubPokemon("bulbasaur", "bulbasaur", 1);

        PokemonDetails bulbasaur = adapter.findDetails("bulbasaur").orElseThrow();

        assertThat(bulbasaur.id()).isEqualTo(1);
        assertThat(bulbasaur.spriteUrl()).isEqualTo(SPRITES + "1.png");
        assertThat(bulbasaur.artworkUrl()).isEqualTo(SPRITES + "other/official-artwork/1.png");
        assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.height()).isEqualTo(Height.ofDecimeters(7));
        assertThat(bulbasaur.weight()).isEqualTo(Weight.ofHectograms(69));
        assertThat(bulbasaur.types()).containsExactly("grass", "poison");
        assertThat(bulbasaur.abilities()).containsExactly(new Ability("overgrow", false), new Ability("chlorophyll", true));
        assertThat(bulbasaur.stats()).containsExactly(
                new BaseStat("hp", 45), new BaseStat("attack", 49), new BaseStat("defense", 49),
                new BaseStat("special-attack", 65), new BaseStat("special-defense", 65), new BaseStat("speed", 45));
    }

    @Test
    void usesTheMostRecentEnglishPokedexEntryOnOneLine() {
        stubPokemon("bulbasaur", "bulbasaur", 1);

        var description = adapter.findDetails("bulbasaur").orElseThrow().description();

        assertThat(description).isEqualTo(
                "While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.");
    }

    @Test
    void buildsALinearEvolutionChainWithLevels() {
        stubPokemon("bulbasaur", "bulbasaur", 1);

        EvolutionNode chain = adapter.findDetails("bulbasaur").orElseThrow().evolutionChain();

        assertThat(chain.names()).containsExactly("bulbasaur", "ivysaur", "venusaur");
        assertThat(chain.spriteUrl()).isEqualTo(SPRITES + "1.png");
        var ivysaur = chain.evolvesTo().getFirst();
        assertThat(ivysaur.speciesId()).isEqualTo(2);
        assertThat(ivysaur.condition()).isEqualTo(new EvolutionCondition("level-up", 16, null));
        assertThat(ivysaur.evolvesTo().getFirst().condition()).isEqualTo(new EvolutionCondition("level-up", 32, null));
    }

    @Test
    void buildsABranchingEvolutionChainWithItems() {
        stubPokemon("eevee", "eevee", 67);

        EvolutionNode chain = adapter.findDetails("eevee").orElseThrow().evolutionChain();

        assertThat(chain.evolvesTo()).hasSize(8);
        assertThat(chain.evolvesTo().getFirst().name()).isEqualTo("vaporeon");
        assertThat(chain.evolvesTo().getFirst().condition()).isEqualTo(new EvolutionCondition("use-item", null, "water-stone"));
    }

    @Test
    void findsAPokemonByPokedexNumber() {
        stubPokemon("1", "bulbasaur", 1);

        assertThat(adapter.findDetails("1")).get().extracting(PokemonDetails::name).isEqualTo("bulbasaur");
    }

    @Test
    void returnsNothingForAnUnknownPokemon() {
        stubFor(get("/pokemon/missingno").willReturn(notFound()));

        assertThat(adapter.findDetails("missingno")).isEmpty();
    }

    @Test
    void reportsCatalogUnavailableWhenThePokemonCallFails() {
        stubFor(get("/pokemon/bulbasaur").willReturn(serverError()));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> adapter.findDetails("bulbasaur"));
    }

    @Test
    void reportsCatalogUnavailableWhenTheEvolutionChainFails() {
        stubPokemon("bulbasaur", "bulbasaur", 1);
        stubFor(get("/evolution-chain/1").willReturn(serverError()));

        assertThatExceptionOfType(CatalogUnavailableException.class)
                .isThrownBy(() -> adapter.findDetails("bulbasaur"));
    }

    private static void stubPokemon(String requestedAs, String name, int chainId) {
        stubFor(get("/pokemon/" + requestedAs).willReturn(okJson(read("pokeapi/pokemon-" + name + ".json"))));
        stubFor(get("/pokemon-species/" + name).willReturn(okJson(read("pokeapi/pokemon-species-" + name + ".json"))));
        stubFor(get("/evolution-chain/" + chainId).willReturn(okJson(read("pokeapi/evolution-chain-" + chainId + ".json"))));
    }
}
