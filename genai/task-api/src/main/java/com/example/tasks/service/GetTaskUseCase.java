package com.example.tasks.service;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;

public interface GetTaskUseCase {

    /** @throws TaskNotFoundException when there is no such task, or it belongs to someone else */
    Task get(UserId caller, TaskId id);
}
