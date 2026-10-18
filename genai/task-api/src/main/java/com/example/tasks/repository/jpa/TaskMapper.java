package com.example.tasks.repository.jpa;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskDescription;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.TaskTitle;
import com.example.tasks.entity.UserId;

/** Mapper between the {@code Task} entity and its row. */
final class TaskMapper {

    private TaskMapper() {
    }

    /** No version yet: tells Spring Data to insert. */
    static TaskJpaEntity toNewRow(Task task) {
        return toRow(task, null);
    }

    /** With the version the task was read at, which Hibernate checks against the stored one. */
    static TaskJpaEntity toExistingRow(Task task) {
        return toRow(task, task.version());
    }

    private static TaskJpaEntity toRow(Task task, Long version) {
        return new TaskJpaEntity(task.id().value(), task.owner().value(), task.title().value(),
                task.description() == null ? null : task.description().value(), task.status(), task.dueDate(),
                task.createdAt(), task.updatedAt(), version);
    }

    static Task toTask(TaskJpaEntity row) {
        return new Task(new TaskId(row.getId()), new UserId(row.getOwnerId()), new TaskTitle(row.getTitle()),
                row.getDescription() == null ? null : new TaskDescription(row.getDescription()), row.getStatus(),
                row.getDueDate(), row.getCreatedAt(), row.getUpdatedAt(), row.getVersion());
    }
}
