package com.lucasamaral.pokedex.web.pagination;

import com.lucasamaral.pokedex.application.pagination.Page;

import java.util.List;
import java.util.function.Function;

/**
 * JSON shape of a paginated response. Pages are zero-based.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<? super S, ? extends T> mapper) {
        Page<T> mapped = page.map(mapper);
        return new PageResponse<>(
                mapped.content(),
                page.query().page(),
                page.query().size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext());
    }
}
