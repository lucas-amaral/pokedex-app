package com.example.tasks.repository.jpa;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskPage;
import com.example.tasks.repository.TaskRepository;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/** Adapter for {@link TaskRepository} on the {@code tasks} table. */
@Component
@Transactional(readOnly = true)
class JpaTaskRepository implements TaskRepository {

    private static final Map<TaskSort.Field, String> COLUMNS = Map.of(
            TaskSort.Field.DUE_DATE, "dueDate",
            TaskSort.Field.CREATED_AT, "createdAt",
            TaskSort.Field.TITLE, "title",
            TaskSort.Field.STATUS, "status");

    private final TaskJpaRepository rows;

    JpaTaskRepository(TaskJpaRepository rows) {
        this.rows = rows;
    }

    /** Flushes at once, so a broken constraint fails here rather than at some later commit. */
    @Override
    @Transactional
    public Task add(Task task) {
        return TaskMapper.toTask(rows.saveAndFlush(TaskMapper.toNewRow(task)));
    }

    /**
     * The service already compared versions, but another request can store a change between that check and this
     * write; Hibernate's version check catches that race.
     */
    @Override
    @Transactional
    public Task update(Task task) {
        try {
            return TaskMapper.toTask(rows.saveAndFlush(TaskMapper.toExistingRow(task)));
        } catch (OptimisticLockingFailureException raceLost) {
            throw new TaskVersionConflictException(task.id(), task.version(), null);
        }
    }

    @Override
    @Transactional
    public void delete(TaskId id, long version) {
        if (rows.deleteByIdAndVersion(id.value(), version) == 0) {
            throw new TaskVersionConflictException(id, version, null);
        }
    }

    @Override
    public TaskPage findPage(UserId owner, TaskFilter filter, TaskSort sort, PageQuery page) {
        var request = PageRequest.of(page.page(), page.size(), toSpringSort(sort));
        var rows = this.rows.findAll(TaskSpecifications.matching(owner, filter), request);
        return new TaskPage(rows.map(TaskMapper::toTask).getContent(), page, rows.getTotalElements());
    }

    /**
     * Tasks without a due date go last in both directions. Creation time and then id break ties, so every task has
     * exactly one place and paging never shows a task twice or skips one.
     */
    private static Sort toSpringSort(TaskSort sort) {
        var direction = sort.direction() == TaskSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        var primary = new Sort.Order(direction, COLUMNS.get(sort.field())).nullsLast();
        return Sort.by(primary, Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    }

    @Override
    public Optional<Task> findByIdAndOwner(TaskId id, UserId owner) {
        return rows.findByIdAndOwnerId(id.value(), owner.value()).map(TaskMapper::toTask);
    }
}
