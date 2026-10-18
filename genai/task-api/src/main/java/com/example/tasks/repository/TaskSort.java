package com.example.tasks.repository;

import com.example.tasks.entity.InvalidParameterException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The order of the task list. Only these fields can be sorted on, so a client can never sort on a column we did not
 * mean to expose (or one that does not exist, which would otherwise be a 500).
 */
public record TaskSort(Field field, Direction direction) {

    public static final TaskSort DEFAULT = new TaskSort(Field.DUE_DATE, Direction.ASC);

    public enum Field {
        DUE_DATE("dueDate"), CREATED_AT("createdAt"), TITLE("title"), STATUS("status");

        private final String apiName;

        Field(String apiName) {
            this.apiName = apiName;
        }

        public String apiName() {
            return apiName;
        }
    }

    public enum Direction { ASC, DESC }

    private static final String ACCEPTED = Arrays.stream(Field.values()).map(Field::apiName).collect(Collectors.joining(", "));
    private static final String RULE = "must be one of " + ACCEPTED + ", optionally followed by ,asc or ,desc";

    /** Reads {@code field} or {@code field,direction}, as in {@code ?sort=title,desc}; empty means the default. */
    public static TaskSort parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        var parts = raw.strip().split(",", -1);
        if (parts.length > 2) {
            throw new InvalidParameterException("sort", RULE);
        }
        var field = Arrays.stream(Field.values()).filter(candidate -> candidate.apiName().equals(parts[0])).findFirst()
                .orElseThrow(() -> new InvalidParameterException("sort", RULE));
        return new TaskSort(field, parts.length == 1 ? Direction.ASC : direction(parts[1]));
    }

    private static Direction direction(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "asc" -> Direction.ASC;
            case "desc" -> Direction.DESC;
            default -> throw new InvalidParameterException("sort", RULE);
        };
    }
}
