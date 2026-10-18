package com.example.tasks.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID>, JpaSpecificationExecutor<TaskJpaEntity> {

    Optional<TaskJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** The version check and the removal in one statement, so nothing can change in between. */
    @Modifying
    @Query("DELETE FROM TaskJpaEntity task WHERE task.id = :id AND task.version = :version")
    int deleteByIdAndVersion(@Param("id") UUID id, @Param("version") long version);
}
