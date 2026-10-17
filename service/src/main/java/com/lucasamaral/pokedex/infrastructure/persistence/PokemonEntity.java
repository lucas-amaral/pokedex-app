package com.lucasamaral.pokedex.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * JPA mapping of a collected Pokémon. Lives only in the persistence adapter; the domain never sees it.
 * <p>
 * Child collections are batch-fetched so a page of Pokémon loads in a handful of queries, not one per row.
 */
@Entity
@Table(name = "pokemon")
class PokemonEntity {

    private static final int BATCH = 50;

    @Id
    UUID id;

    @Column(name = "pokeapi_id", nullable = false, unique = true)
    int pokeapiId;

    @Column(nullable = false)
    String name;

    @Column(name = "species_name", nullable = false)
    String speciesName;

    String category;

    String description;

    @Column(name = "sprite_url")
    String spriteUrl;

    @Column(name = "artwork_url")
    String artworkUrl;

    @Column(name = "height_dm", nullable = false)
    int heightDm;

    @Column(name = "weight_hg", nullable = false)
    int weightHg;

    @ElementCollection
    @CollectionTable(name = "pokemon_type", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "slot")
    @Column(name = "name", nullable = false)
    @BatchSize(size = BATCH)
    List<String> types = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pokemon_ability", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @BatchSize(size = BATCH)
    List<AbilityValue> abilities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pokemon_stat", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @BatchSize(size = BATCH)
    List<StatValue> stats = new ArrayList<>();

    String nickname;

    String notes;

    String region;

    @ElementCollection
    @CollectionTable(name = "pokemon_localized_name", joinColumns = @JoinColumn(name = "pokemon_id"))
    @MapKeyColumn(name = "language")
    @Column(name = "name", nullable = false)
    @BatchSize(size = BATCH)
    Map<String, String> localizedNames = new TreeMap<>();

    @ElementCollection
    @CollectionTable(name = "pokemon_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "tag", nullable = false)
    @BatchSize(size = BATCH)
    Set<String> tags = new HashSet<>();

    @Column(name = "synced_at", nullable = false)
    Instant syncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    Long version;

    @Embeddable
    record AbilityValue(@Column(name = "name", nullable = false) String name,
                        @Column(name = "hidden", nullable = false) boolean hidden) {
    }

    @Embeddable
    record StatValue(@Column(name = "name", nullable = false) String name,
                     @Column(name = "base_value", nullable = false) int baseValue) {
    }
}
