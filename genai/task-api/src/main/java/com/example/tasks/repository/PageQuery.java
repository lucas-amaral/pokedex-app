package com.example.tasks.repository;

/** One page of results; pages count from 0. */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or more, was " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be 1 to " + MAX_SIZE + ", was " + size);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
