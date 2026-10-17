package com.lucasamaral.pokedex.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface PokemonJpaRepository extends JpaRepository<PokemonEntity, UUID>, JpaSpecificationExecutor<PokemonEntity> {

    Optional<PokemonEntity> findByPokeapiId(int pokeapiId);
}
