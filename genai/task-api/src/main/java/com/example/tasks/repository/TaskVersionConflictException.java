package com.example.tasks.repository;

import com.example.tasks.entity.TaskId;

import java.util.Optional;

/**
 * A change started from a version that is no longer the stored one: someone else changed the task first.
 *
 * @param currentVersion the stored version when known; empty when the database detected the race itself
 */
public class TaskVersionConflictException extends RuntimeException {

    private final long expectedVersion;
    private final Long currentVersion;

    public TaskVersionConflictException(TaskId id, long expectedVersion, Long currentVersion) {
        super("Task '" + id.value() + "' is no longer at version " + expectedVersion);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public Optional<Long> currentVersion() {
        return Optional.ofNullable(currentVersion);
    }
}
