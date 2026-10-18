package com.lucasamaral.pokedex.application.seed;

import com.lucasamaral.pokedex.application.catalog.PokemonDetailsCatalog;
import com.lucasamaral.pokedex.application.collection.EditCollectionService;
import com.lucasamaral.pokedex.application.collection.SyncPokemonService;
import com.lucasamaral.pokedex.application.user.PasswordHasher;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import com.lucasamaral.pokedex.domain.pokemon.PokemonDetails;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.Role;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;
import com.lucasamaral.pokedex.support.InMemoryCollectionRepository;
import com.lucasamaral.pokedex.support.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.lucasamaral.pokedex.support.PokemonDetailsBuilder.aPokemonDetails;
import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private static final DemoAccount OAK = new DemoAccount("oak", "oak@pallet.town", "professor123", Role.ADMIN);
    private static final DemoAccount ASH = new DemoAccount("ash", "ash@pallet.town", "pikachu123", Role.USER);

    private final SnapshotCatalog snapshot = new SnapshotCatalog();
    private final InMemoryCollectionRepository collection = new InMemoryCollectionRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final DemoDataSeeder seeder = new DemoDataSeeder(
            new SyncPokemonService(snapshot, collection, clock), new EditCollectionService(collection), collection,
            users, new PrefixPasswordHasher(), clock);

    @Test
    void copiesEveryPokemonOfTheSnapshotIntoAnEmptyCollection() {
        snapshot.add(aPokemonDetails().withId(1).withName("bulbasaur").withSpeciesName("bulbasaur").build());
        snapshot.add(aPokemonDetails().withId(152).withName("chikorita").withSpeciesName("chikorita").build());

        var report = seeder.seed(List.of(), List.of("1", "152"));

        assertThat(report.pokemonAdded()).isEqualTo(2);
        assertThat(collection.all()).extracting(CollectedPokemon::pokeapiId).containsExactlyInAnyOrder(1, 152);
    }

    @Test
    void givesEachPokemonItsRegionAndGenerationTag() {
        snapshot.add(aPokemonDetails().withId(19).withName("rattata").withSpeciesName("rattata").build());
        snapshot.add(aPokemonDetails().withId(161).withName("sentret").withSpeciesName("sentret").build());

        seeder.seed(List.of(), List.of("19", "161"));

        var rattata = collection.findByPokeapiId(19).orElseThrow().custom();
        assertThat(rattata.region()).isEqualTo("Kanto");
        assertThat(rattata.tags()).containsExactly("gen-1");
        var sentret = collection.findByPokeapiId(161).orElseThrow().custom();
        assertThat(sentret.region()).isEqualTo("Johto");
        assertThat(sentret.tags()).containsExactly("gen-2");
    }

    @Test
    void marksStartersLegendariesAndMythicals() {
        snapshot.add(aPokemonDetails().withId(4).withName("charmander").withSpeciesName("charmander").build());
        snapshot.add(aPokemonDetails().withId(150).withName("mewtwo").withSpeciesName("mewtwo").build());
        snapshot.add(aPokemonDetails().withId(251).withName("celebi").withSpeciesName("celebi").build());

        seeder.seed(List.of(), List.of("4", "150", "251"));

        assertThat(collection.findByPokeapiId(4).orElseThrow().custom().tags()).containsExactly("gen-1", "starter");
        assertThat(collection.findByPokeapiId(150).orElseThrow().custom().tags()).containsExactly("gen-1", "legendary");
        assertThat(collection.findByPokeapiId(251).orElseThrow().custom().tags()).containsExactly("gen-2", "mythical");
    }

    @Test
    void addsLocalizedNamesToTheStarters() {
        snapshot.add(aPokemonDetails().withId(1).withName("bulbasaur").withSpeciesName("bulbasaur").build());

        seeder.seed(List.of(), List.of("1"));

        assertThat(collection.findByPokeapiId(1).orElseThrow().custom().localizedNames())
                .isEqualTo(Map.of("de", "Bisasam", "fr", "Bulbizarre", "ja", "フシギダネ"));
    }

    @Test
    void leavesACollectionThatAlreadyHasPokemonAlone() {
        collection.save(CollectedPokemon.collect(aPokemonDetails().withId(25).withName("pikachu").withSpeciesName("pikachu").build(), NOW));
        snapshot.add(aPokemonDetails().withId(1).withName("bulbasaur").withSpeciesName("bulbasaur").build());

        var report = seeder.seed(List.of(), List.of("1"));

        assertThat(report.pokemonAdded()).isZero();
        assertThat(collection.all()).extracting(CollectedPokemon::pokeapiId).containsExactly(25);
    }

    @Test
    void createsTheDemoAccountsWithTheirRoles() {
        var report = seeder.seed(List.of(OAK, ASH), List.of());

        assertThat(report.accountsCreated()).isEqualTo(2);
        var oak = users.findByUsername(Username.of("oak")).orElseThrow();
        assertThat(oak.role()).isEqualTo(Role.ADMIN);
        assertThat(oak.email()).isEqualTo(Email.of("oak@pallet.town"));
        assertThat(oak.passwordHash()).isEqualTo("hashed:professor123");
        assertThat(oak.createdAt()).isEqualTo(NOW);
        assertThat(users.findByUsername(Username.of("ash")).orElseThrow().role()).isEqualTo(Role.USER);
    }

    @Test
    void keepsAccountsThatAlreadyExist() {
        var existing = users.save(new User(UUID.randomUUID(), Username.of("ash"), Email.of("ash@pallet.town"),
                "hashed:changed-by-ash", Role.USER, NOW.minusSeconds(3600)));

        var report = seeder.seed(List.of(OAK, ASH), List.of());

        assertThat(report.accountsCreated()).isEqualTo(1);
        assertThat(users.findByUsername(Username.of("ash"))).contains(existing);
    }

    @Test
    void skipsAnAccountWhoseEmailIsAlreadyInUse() {
        users.save(new User(UUID.randomUUID(), Username.of("gary"), Email.of("oak@pallet.town"), "hashed:x", Role.USER, NOW));

        var report = seeder.seed(List.of(OAK), List.of());

        assertThat(report.accountsCreated()).isZero();
        assertThat(users.findByUsername(Username.of("oak"))).isEmpty();
    }

    private static final class SnapshotCatalog implements PokemonDetailsCatalog {

        private final List<PokemonDetails> pokemon = new ArrayList<>();

        void add(PokemonDetails details) {
            pokemon.add(details);
        }

        @Override
        public Optional<PokemonDetails> findDetails(String idOrName) {
            return pokemon.stream()
                    .filter(details -> idOrName.equals(String.valueOf(details.id())) || idOrName.equals(details.name()))
                    .findFirst();
        }
    }

    private static final class PrefixPasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals(hash(rawPassword));
        }
    }
}
