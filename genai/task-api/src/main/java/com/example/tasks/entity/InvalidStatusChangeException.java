package com.example.tasks.entity;

import java.util.List;

/** A well-formed request that breaks the transition table. */
public class InvalidStatusChangeException extends RuntimeException {

    private final TaskStatus from;
    private final TaskStatus to;

    public InvalidStatusChangeException(TaskStatus from, TaskStatus to) {
        super("A task cannot go from " + from + " to " + to + ".");
        this.from = from;
        this.to = to;
    }

    public TaskStatus from() {
        return from;
    }

    public TaskStatus to() {
        return to;
    }

    /** Where the task could go instead, in a stable order. */
    public List<TaskStatus> allowed() {
        return List.copyOf(from.nextStatuses());
    }
}
