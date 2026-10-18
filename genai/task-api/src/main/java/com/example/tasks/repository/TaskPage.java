package com.example.tasks.repository;

import com.example.tasks.entity.Task;

import java.util.List;

public record TaskPage(List<Task> tasks, PageQuery query, long totalElements) {

    public TaskPage {
        tasks = List.copyOf(tasks);
    }

    public long totalPages() {
        return (totalElements + query.size() - 1) / query.size();
    }
}
