package com.lucasamaral.pokedex.application.collection;

import java.util.UUID;

/**
 * An edit was based on an outdated copy: someone else changed this Pokémon in the meantime.
 */
public class CollectedPokemonVersionConflictException extends RuntimeException {

    private final long expectedVersion;
    private final long currentVersion;

    public CollectedPokemonVersionConflictException(UUID id, long expectedVersion, long currentVersion) {
        super("Pokémon '" + id + "' changed since version " + expectedVersion + "; it is now at version " + currentVersion);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long currentVersion() {
        return currentVersion;
    }
}
