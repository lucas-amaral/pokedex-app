package com.example.tasks.service;

import com.example.tasks.entity.TaskStatus;

/** The status a client asks for, plus the version it read. */
public record StatusChange(long version, TaskStatus status) {
}
