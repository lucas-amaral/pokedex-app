package com.example.tasks.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aggregate: a task and the rules about it. It is immutable; every change returns a new version of it.
 *
 * @param description {@code null} when the task has none
 * @param dueDate     {@code null} when the task has none
 * @param version     how many times the stored task has changed; a change must start from the current one
 */
public record Task(
        TaskId id,
        UserId owner,
        TaskTitle title,
        TaskDescription description,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public Task {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(owner, "owner is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (version < 0) {
            throw new IllegalArgumentException("version must be zero or more, was " + version);
        }
    }

    /**
     * Static Factory for a new task: it starts as TODO, and cannot be created already overdue.
     *
     * @param today the current date, in the zone the API judges due dates by
     */
    public static Task create(UserId owner, TaskTitle title, TaskDescription description, LocalDate dueDate,
                              Instant now, LocalDate today) {
        if (dueDate != null && dueDate.isBefore(today)) {
            throw new InvalidFieldException("dueDate", "must be today or later");
        }
        return new Task(TaskId.random(), owner, title, description, TaskStatus.TODO, dueDate, now, now, 0);
    }

    /**
     * Replaces the editable fields (status has its own rules and its own operation). The due date may not move into
     * the past, but keeping one that has passed is fine, so an overdue task stays editable.
     */
    public Task edit(TaskTitle newTitle, TaskDescription newDescription, LocalDate newDueDate, Instant now, LocalDate today) {
        var dueDateChanges = !Objects.equals(newDueDate, dueDate);
        if (dueDateChanges && newDueDate != null && newDueDate.isBefore(today)) {
            throw new InvalidFieldException("dueDate", "must be today or later");
        }
        return new Task(id, owner, newTitle, newDescription, status, newDueDate, createdAt, now, version);
    }

    /** Returns this same task when the status is already {@code target}, so a retried request changes nothing. */
    public Task changeStatus(TaskStatus target, Instant now) {
        if (target == status) {
            return this;
        }
        if (!status.canMoveTo(target)) {
            throw new InvalidStatusChangeException(status, target);
        }
        return new Task(id, owner, title, description, target, dueDate, createdAt, now, version);
    }

    public boolean isOwnedBy(UserId user) {
        return owner.equals(user);
    }
}
