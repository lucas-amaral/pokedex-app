package com.example.tasks.service;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.TaskDescription;
import com.example.tasks.entity.TaskId;
import com.example.tasks.entity.TaskTitle;
import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskPage;
import com.example.tasks.repository.TaskRepository;
import com.example.tasks.repository.TaskSort;
import com.example.tasks.repository.TaskVersionConflictException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TaskService implements CreateTaskUseCase, GetTaskUseCase, ListTasksUseCase, UpdateTaskUseCase,
        ChangeTaskStatusUseCase, DeleteTaskUseCase {

    private final TaskRepository tasks;
    private final Clock clock;

    public TaskService(TaskRepository tasks, Clock clock) {
        this.tasks = Objects.requireNonNull(tasks, "tasks is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public Task create(UserId owner, NewTask newTask) {
        var now = now();
        var task = Task.create(owner, TaskTitle.of(newTask.title()), TaskDescription.of(newTask.description()).orElse(null),
                newTask.dueDate(), now, today(now));
        return tasks.add(task);
    }

    @Override
    public Task get(UserId caller, TaskId id) {
        return tasks.findByIdAndOwner(id, caller).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public TaskPage list(UserId caller, TaskFilter filter, TaskSort sort, PageQuery page) {
        return tasks.findPage(caller, filter, sort, page);
    }

    @Override
    public Task update(UserId caller, TaskId id, TaskChanges changes) {
        var stored = get(caller, id);
        if (stored.version() != changes.version()) {
            throw new TaskVersionConflictException(id, changes.version(), stored.version());
        }
        var now = now();
        var edited = stored.edit(TaskTitle.of(changes.title()), TaskDescription.of(changes.description()).orElse(null),
                changes.dueDate(), now, today(now));
        return tasks.update(edited);
    }

    @Override
    public Task changeStatus(UserId caller, TaskId id, StatusChange change) {
        var stored = get(caller, id);
        if (stored.version() != change.version()) {
            throw new TaskVersionConflictException(id, change.version(), stored.version());
        }
        var changed = stored.changeStatus(change.status(), now());
        return changed == stored ? stored : tasks.update(changed);
    }

    @Override
    public void delete(UserId caller, TaskId id, long version) {
        var stored = get(caller, id);
        if (stored.version() != version) {
            throw new TaskVersionConflictException(id, version, stored.version());
        }
        tasks.delete(id, version);
    }

    /** PostgreSQL keeps microseconds; truncating makes what we return equal what a later read returns. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** Due dates are judged by the UTC date (decided before implementation). */
    private static LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, ZoneOffset.UTC);
    }
}
