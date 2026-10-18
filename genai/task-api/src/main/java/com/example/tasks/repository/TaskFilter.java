package com.example.tasks.repository;

import com.example.tasks.entity.InvalidParameterException;
import com.example.tasks.entity.TaskStatus;

import java.time.LocalDate;

/**
 * Optional filters for the task list; {@code null} means "any". The due date range is inclusive, and tasks without a
 * due date are left out as soon as either end is given.
 */
public record TaskFilter(TaskStatus status, LocalDate dueFrom, LocalDate dueTo) {

    public static final TaskFilter NONE = new TaskFilter(null, null, null);

    public TaskFilter {
        if (dueFrom != null && dueTo != null && dueTo.isBefore(dueFrom)) {
            throw new InvalidParameterException("dueTo", "must not be before dueFrom");
        }
    }
}
