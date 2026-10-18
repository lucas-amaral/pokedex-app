package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.collection.CollectedPokemonVersionConflictException;
import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.DuplicateCollectedPokemonException;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: the collection stored in PostgreSQL through Spring Data JPA.
 */
@Repository
@Transactional(readOnly = true)
class JpaCollectionRepository implements CollectionRepository {

    private static final String UNIQUE_POKEAPI_ID = "uk_pokemon_pokeapi_id";

    private final PokemonJpaRepository jpa;

    JpaCollectionRepository(PokemonJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CollectedPokemon> findById(UUID id) {
        return jpa.findById(id).map(PokemonEntityMapper::toDomain);
    }

    @Override
    public Optional<CollectedPokemon> findByPokeapiId(int pokeapiId) {
        return jpa.findByPokeapiId(pokeapiId).map(PokemonEntityMapper::toDomain);
    }

    /**
     * Inserts or updates, and returns the Pokémon as stored (with the version the database now holds).
     * An update must start from the stored version; a change made from an outdated copy is refused.
     */
    @Override
    @Transactional
    public CollectedPokemon save(CollectedPokemon pokemon) {
        var stored = jpa.findById(pokemon.id());
        if (stored.isPresent() && stored.get().version != pokemon.version()) {
            throw new CollectedPokemonVersionConflictException(pokemon.id(), pokemon.version(), stored.get().version);
        }
        var entity = stored.orElseGet(PokemonEntity::new);
        PokemonEntityMapper.copyInto(pokemon, entity);
        try {
            return PokemonEntityMapper.toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            if (violates(e, UNIQUE_POKEAPI_ID)) {
                throw new DuplicateCollectedPokemonException(pokemon.pokeapiId(), e);
            }
            throw e;
        } catch (OptimisticLockingFailureException e) {
            // Another transaction updated the row between our read and our write: at least one newer version exists.
            throw new CollectedPokemonVersionConflictException(pokemon.id(), pokemon.version(), pokemon.version() + 1);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public Page<CollectedPokemon> findPage(CollectionFilter filter, PageQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by("pokeapiId"));
        var page = jpa.findAll(PokemonSpecifications.matching(filter), pageable);
        return new Page<>(page.getContent().stream().map(PokemonEntityMapper::toDomain).toList(), query, page.getTotalElements());
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        return exception.getCause() instanceof ConstraintViolationException violation
                && constraint.equalsIgnoreCase(violation.getConstraintName());
    }
}
