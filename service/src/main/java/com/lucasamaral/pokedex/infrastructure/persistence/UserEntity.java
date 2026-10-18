package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.domain.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping of a user account. Users are only created, never edited here, so there is no version column.
 */
@Entity
@Table(name = "users")
class UserEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String username;

    @Column(nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
