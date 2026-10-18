package com.lucasamaral.pokedex.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void neverStoresThePasswordItself() {
        var hash = hasher.hash("pikachu123");

        assertThat(hash).startsWith("$2").doesNotContain("pikachu123");
    }

    @Test
    void recognizesTheRightPasswordOnly() {
        var hash = hasher.hash("pikachu123");

        assertThat(hasher.matches("pikachu123", hash)).isTrue();
        assertThat(hasher.matches("pikachu124", hash)).isFalse();
    }

    @Test
    void saltsEveryHash() {
        assertThat(hasher.hash("pikachu123")).isNotEqualTo(hasher.hash("pikachu123"));
    }
}
