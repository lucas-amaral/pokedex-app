package com.example.tasks.controller;

import com.example.tasks.repository.TaskPage;

import java.util.List;

/** One page of tasks with what a client needs to page through the rest. */
public record PageResponse(List<TaskResponse> content, int page, int size, long totalElements, long totalPages) {

    static PageResponse from(TaskPage page) {
        return new PageResponse(page.tasks().stream().map(TaskResponse::from).toList(), page.query().page(),
                page.query().size(), page.totalElements(), page.totalPages());
    }
}
