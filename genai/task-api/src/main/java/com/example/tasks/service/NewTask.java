package com.example.tasks.service;

import java.time.LocalDate;

/** What a client sends to create a task, before the entity's rules have checked it. */
public record NewTask(String title, String description, LocalDate dueDate) {
}
