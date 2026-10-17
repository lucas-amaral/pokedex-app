package com.lucasamaral.pokedex.application.pagination;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageTest {

    @Test
    void roundsTotalPagesUp() {
        var page = new Page<>(List.of("a", "b"), new PageQuery(0, 20), 1025);

        assertThat(page.totalPages()).isEqualTo(52);
    }

    @Test
    void emptyCatalogHasNoPages() {
        var page = new Page<String>(List.of(), new PageQuery(0, 20), 0);

        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void hasNextUntilTheLastPage() {
        assertThat(new Page<>(List.of("a"), new PageQuery(0, 1), 2).hasNext()).isTrue();
        assertThat(new Page<>(List.of("b"), new PageQuery(1, 1), 2).hasNext()).isFalse();
    }

    @Test
    void mapsContentAndKeepsPaginationData() {
        var page = new Page<>(List.of(1, 2), new PageQuery(3, 2), 10);

        var mapped = page.map(number -> "#" + number);

        assertThat(mapped.content()).containsExactly("#1", "#2");
        assertThat(mapped.query()).isEqualTo(page.query());
        assertThat(mapped.totalElements()).isEqualTo(10);
    }

    @Test
    void isNotAffectedByChangesToTheOriginalContent() {
        var content = new ArrayList<>(List.of("a"));
        var page = new Page<>(content, new PageQuery(0, 20), 1);

        content.clear();

        assertThat(page.content()).containsExactly("a");
    }

    @Test
    void rejectsNegativeTotal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Page<>(List.of(), new PageQuery(0, 20), -1))
                .withMessageContaining("total");
    }
}
