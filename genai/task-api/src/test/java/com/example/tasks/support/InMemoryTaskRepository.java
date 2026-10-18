package com.example.tasks.support;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskPage;
import com.example.tasks.repository.TaskRepository;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake of the {@link TaskRepository} port, so service tests check behaviour without a database or mocks. */
public final class InMemoryTaskRepository implements TaskRepository {

    private final Map<TaskId, Task> tasks = new LinkedHashMap<>();

    @Override
    public Task add(Task task) {
        if (tasks.containsKey(task.id())) {
            throw new IllegalStateException("Task " + task.id() + " already exists");
        }
        tasks.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findByIdAndOwner(TaskId id, UserId owner) {
        return Optional.ofNullable(tasks.get(id)).filter(task -> task.isOwnedBy(owner));
    }

    /** Like the database: the change must start from the stored version, which then goes up by one. */
    @Override
    public Task update(Task task) {
        var stored = tasks.get(task.id());
        if (stored == null || stored.version() != task.version()) {
            throw new TaskVersionConflictException(task.id(), task.version(), stored == null ? null : stored.version());
        }
        var updated = new Task(task.id(), task.owner(), task.title(), task.description(), task.status(), task.dueDate(),
                task.createdAt(), task.updatedAt(), task.version() + 1);
        tasks.put(task.id(), updated);
        return updated;
    }

    @Override
    public void delete(TaskId id, long version) {
        var stored = tasks.get(id);
        if (stored == null || stored.version() != version) {
            throw new TaskVersionConflictException(id, version, stored == null ? null : stored.version());
        }
        tasks.remove(id);
    }

    /** Only what service tests need: the owner and the page. Filters and order are tested against PostgreSQL. */
    @Override
    public TaskPage findPage(UserId owner, TaskFilter filter, TaskSort sort, PageQuery page) {
        var owned = tasks.values().stream().filter(task -> task.isOwnedBy(owner)).toList();
        var content = owned.stream().skip(page.offset()).limit(page.size()).toList();
        return new TaskPage(content, page, owned.size());
    }

    public List<Task> all() {
        return List.copyOf(tasks.values());
    }
}
