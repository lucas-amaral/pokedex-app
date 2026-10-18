package com.example.tasks.service;

import com.example.tasks.entity.UserId;
import com.example.tasks.repository.PageQuery;
import com.example.tasks.repository.TaskFilter;
import com.example.tasks.repository.TaskPage;
import com.example.tasks.repository.TaskSort;

public interface ListTasksUseCase {

    /** Only ever the caller's own tasks. */
    TaskPage list(UserId caller, TaskFilter filter, TaskSort sort, PageQuery page);
}
