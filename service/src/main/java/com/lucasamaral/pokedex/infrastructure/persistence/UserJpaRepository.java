package com.lucasamaral.pokedex.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
