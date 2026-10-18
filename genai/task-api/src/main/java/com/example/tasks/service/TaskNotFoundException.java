package com.example.tasks.service;

import com.example.tasks.entity.TaskId;

/** Also used for another user's task, so a client cannot tell the two apart. */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(TaskId id) {
        super("No task with id '" + id.value() + "'");
    }
}
