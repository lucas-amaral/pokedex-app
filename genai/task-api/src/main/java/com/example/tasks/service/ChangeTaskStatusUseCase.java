package com.example.tasks.service;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;

public interface ChangeTaskStatusUseCase {

    /**
     * Asking for the current status again returns the task unchanged, without a new version.
     *
     * @throws TaskNotFoundException when there is no such task among the caller's
     * @throws com.example.tasks.repository.TaskVersionConflictException when the task changed since {@code change.version()}
     * @throws com.example.tasks.entity.InvalidStatusChangeException when the transition is not allowed
     */
    Task changeStatus(UserId caller, TaskId id, StatusChange change);
}
