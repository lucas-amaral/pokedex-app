package com.lucasamaral.pokedex.application.pagination;

/**
 * A request for one page of results. Pages are zero-based.
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater, was " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE + ", was " + size);
        }
    }

    public static PageQuery first(int size) {
        return new PageQuery(0, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
