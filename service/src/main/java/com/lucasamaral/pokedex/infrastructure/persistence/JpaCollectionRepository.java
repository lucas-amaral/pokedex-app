package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import com.lucasamaral.pokedex.application.collection.CollectionRepository;
import com.lucasamaral.pokedex.application.collection.DuplicateCollectedPokemonException;
import com.lucasamaral.pokedex.application.pagination.Page;
import com.lucasamaral.pokedex.application.pagination.PageQuery;
import com.lucasamaral.pokedex.domain.collection.CollectedPokemon;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Override
    @Transactional
    public CollectedPokemon save(CollectedPokemon pokemon) {
        var entity = jpa.findById(pokemon.id()).orElseGet(PokemonEntity::new);
        PokemonEntityMapper.copyInto(pokemon, entity);
        try {
            jpa.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (violates(e, UNIQUE_POKEAPI_ID)) {
                throw new DuplicateCollectedPokemonException(pokemon.pokeapiId(), e);
            }
            throw e;
        }
        return pokemon;
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
