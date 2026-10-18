package com.example.tasks.service;

import java.time.LocalDate;

/** A full replacement of the editable fields, plus the version the client read. */
public record TaskChanges(long version, String title, String description, LocalDate dueDate) {
}
