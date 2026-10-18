package com.example.tasks.repository.jpa;

import com.example.tasks.entity.UserId;
import com.example.tasks.repository.TaskFilter;
import org.springframework.data.jpa.domain.Specification;

/** Specification: the list query built from optional parts, always starting with the owner. */
final class TaskSpecifications {

    private TaskSpecifications() {
    }

    static Specification<TaskJpaEntity> matching(UserId owner, TaskFilter filter) {
        Specification<TaskJpaEntity> query = (task, criteria, builder) -> builder.equal(task.get("ownerId"), owner.value());
        if (filter.status() != null) {
            query = query.and((task, criteria, builder) -> builder.equal(task.get("status"), filter.status()));
        }
        if (filter.dueFrom() != null) {
            query = query.and((task, criteria, builder) -> builder.greaterThanOrEqualTo(task.get("dueDate"), filter.dueFrom()));
        }
        if (filter.dueTo() != null) {
            query = query.and((task, criteria, builder) -> builder.lessThanOrEqualTo(task.get("dueDate"), filter.dueTo()));
        }
        return query;
    }
}
