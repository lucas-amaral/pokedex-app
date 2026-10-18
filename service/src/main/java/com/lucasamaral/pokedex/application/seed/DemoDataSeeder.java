package com.lucasamaral.pokedex.application.seed;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.SyncPokemonUseCase;
import com.lucasamaral.pokedex.application.collection.UpdateCollectedPokemonUseCase;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.application.user.PasswordHasher;
import com.lucasamaral.pokedex.application.user.UserRepository;
import com.lucasamaral.pokedex.domain.user.Email;
import com.lucasamaral.pokedex.domain.user.User;
import com.lucasamaral.pokedex.domain.user.Username;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fills an empty installation with demo data: accounts to log in with, and a collection to browse.
 * <p>
 * Pokémon go in through the same sync and update use cases the API uses, so seeded rows obey the same rules. Which
 * catalog the sync reads from is the caller's choice (a bundled snapshot at startup, so no PokeAPI call is needed).
 * <p>
 * Safe to run on every start: existing accounts are kept, and a collection that already has Pokémon is left alone,
 * so nothing removed or edited by users comes back.
 */
public class DemoDataSeeder {

    private final SyncPokemonUseCase sync;
    private final UpdateCollectedPokemonUseCase update;
    private final CollectionRepository collection;
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public DemoDataSeeder(SyncPokemonUseCase sync, UpdateCollectedPokemonUseCase update, CollectionRepository collection,
                          UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.sync = Objects.requireNonNull(sync, "sync is required");
        this.update = Objects.requireNonNull(update, "update is required");
        this.collection = Objects.requireNonNull(collection, "collection is required");
        this.users = Objects.requireNonNull(users, "users is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /**
     * @param accounts accounts to create when neither their username nor their email is taken
     * @param pokemon  ids or names to collect when the collection is empty
     */
    public SeedReport seed(List<DemoAccount> accounts, List<String> pokemon) {
        return new SeedReport(createAccounts(accounts), fillEmptyCollection(pokemon));
    }

    private int createAccounts(List<DemoAccount> accounts) {
        var created = 0;
        for (var account : accounts) {
            var username = Username.of(account.username());
            var email = Email.of(account.email());
            if (users.existsByUsername(username) || users.existsByEmail(email)) {
                continue;
            }
            var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            users.save(new User(UUID.randomUUID(), username, email, passwordHasher.hash(account.password()), account.role(), now));
            created++;
        }
        return created;
    }

    private int fillEmptyCollection(List<String> pokemon) {
        if (collection.findPage(CollectionFilter.none(), PageQuery.first(1)).totalElements() > 0) {
            return 0;
        }
        var added = 0;
        for (var idOrName : pokemon) {
            var result = sync.sync(idOrName);
            var stored = result.pokemon();
            update.update(stored.id(), stored.version(), DemoFields.forPokemon(stored.pokeapiId()));
            if (result.created()) {
                added++;
            }
        }
        return added;
    }
}
