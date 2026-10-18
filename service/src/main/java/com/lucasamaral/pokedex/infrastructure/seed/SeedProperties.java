package com.lucasamaral.pokedex.infrastructure.seed;

import com.lucasamaral.pokedex.application.seed.DemoAccount;
import com.lucasamaral.pokedex.domain.user.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Demo data loaded at startup when {@code enabled}.
 *
 * @param snapshot classpath location of the PokeAPI snapshot to collect
 * @param accounts accounts to create
 */
@ConfigurationProperties("pokedex.seed")
public record SeedProperties(
        boolean enabled,
        @DefaultValue(DEFAULT_SNAPSHOT) String snapshot,
        @DefaultValue List<Account> accounts) {

    public static final String DEFAULT_SNAPSHOT = "seed/pokemon-gen1-2.json";

    public record Account(String username, String email, String password, Role role) {

        DemoAccount toDemoAccount() {
            return new DemoAccount(username, email, password, role);
        }

        @Override
        public String toString() {
            return "Account[username=" + username + ", role=" + role + "]";
        }
    }

    List<DemoAccount> demoAccounts() {
        return accounts.stream().map(Account::toDemoAccount).toList();
    }
}
