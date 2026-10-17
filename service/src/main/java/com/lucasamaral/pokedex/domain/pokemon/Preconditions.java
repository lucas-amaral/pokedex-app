package com.lucasamaral.pokedex.domain.pokemon;

/**
 * Invariant checks shared by the Pokémon value objects.
 */
final class Preconditions {

    private Preconditions() {
    }

    static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message + ": " + value);
        }
        return value;
    }
}
