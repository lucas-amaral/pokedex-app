package com.example.tasks.service;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;

public interface UpdateTaskUseCase {

    /**
     * @throws TaskNotFoundException when there is no such task among the caller's
     * @throws com.example.tasks.repository.TaskVersionConflictException when the task changed since {@code changes.version()}
     * @throws com.example.tasks.entity.InvalidFieldException when a field breaks a rule
     */
    Task update(UserId caller, TaskId id, TaskChanges changes);
}
