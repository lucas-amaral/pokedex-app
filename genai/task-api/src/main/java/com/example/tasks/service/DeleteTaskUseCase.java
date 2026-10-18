package com.example.tasks.service;

import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;

public interface DeleteTaskUseCase {

    /**
     * Removes the task for good; deleting it again is a {@link TaskNotFoundException}.
     *
     * @throws TaskNotFoundException when there is no such task among the caller's
     * @throws com.example.tasks.repository.TaskVersionConflictException when the task changed since {@code version}
     */
    void delete(UserId caller, TaskId id, long version);
}
