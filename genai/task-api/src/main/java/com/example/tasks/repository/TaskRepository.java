package com.example.tasks.repository;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;

import java.util.Optional;

/** Port: where tasks are stored. Lookups always include the owner, so another user's task is never even loaded. */
public interface TaskRepository {

    /** Stores a new task and returns it as stored. */
    Task add(Task task);

    /**
     * Stores a changed task, which must carry the version it was read at; returns it with the next version.
     *
     * @throws TaskVersionConflictException when the stored task has changed since
     */
    Task update(Task task);

    /**
     * Removes the task for good, but only at the given version.
     *
     * @throws TaskVersionConflictException when the stored task is no longer at that version, or is gone
     */
    void delete(TaskId id, long version);

    Optional<Task> findByIdAndOwner(TaskId id, UserId owner);

    /** The owner's tasks matching the filter, in the given order, one page at a time. */
    TaskPage findPage(UserId owner, TaskFilter filter, TaskSort sort, PageQuery page);
}
