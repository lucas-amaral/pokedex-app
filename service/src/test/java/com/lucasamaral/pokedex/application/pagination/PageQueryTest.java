package com.lucasamaral.pokedex.application.pagination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageQueryTest {

    @Test
    void computesOffsetFromPageAndSize() {
        assertThat(new PageQuery(2, 20).offset()).isEqualTo(40);
    }

    @Test
    void firstPageStartsAtZero() {
        assertThat(PageQuery.first(20).offset()).isZero();
    }

    @Test
    void rejectsNegativePage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PageQuery(-1, 20))
                .withMessageContaining("page");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, PageQuery.MAX_SIZE + 1})
    void rejectsSizeOutsideAllowedRange(int size) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PageQuery(0, size))
                .withMessageContaining("size");
    }

    @Test
    void acceptsMaximumSize() {
        assertThat(new PageQuery(0, PageQuery.MAX_SIZE).size()).isEqualTo(PageQuery.MAX_SIZE);
    }
}
