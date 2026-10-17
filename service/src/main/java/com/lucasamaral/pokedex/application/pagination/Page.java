package com.lucasamaral.pokedex.application.pagination;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One page of results plus what a client needs to navigate to the others.
 */
public record Page<T>(List<T> content, PageQuery query, long totalElements) {

    public Page {
        content = List.copyOf(Objects.requireNonNull(content, "content is required"));
        Objects.requireNonNull(query, "query is required");
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be zero or greater, was " + totalElements);
        }
    }

    public int totalPages() {
        return (int) Math.ceilDiv(totalElements, query.size());
    }

    public boolean hasNext() {
        return query.page() < totalPages() - 1;
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(content.stream().<R>map(mapper).toList(), query, totalElements);
    }
}
