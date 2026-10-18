package com.example.tasks.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * The status of a task and where it may go next (the transition table agreed before implementation). Keeping the table on the
 * enum, a lightweight form of the State pattern, puts every rule about status in one place.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    /** A task can be finished without being started, paused, or reopened; a reopened task is in progress again. */
    public Set<TaskStatus> nextStatuses() {
        return switch (this) {
            case TODO -> EnumSet.of(IN_PROGRESS, DONE);
            case IN_PROGRESS -> EnumSet.of(TODO, DONE);
            case DONE -> EnumSet.of(IN_PROGRESS);
        };
    }

    public boolean canMoveTo(TaskStatus target) {
        return nextStatuses().contains(target);
    }
}
