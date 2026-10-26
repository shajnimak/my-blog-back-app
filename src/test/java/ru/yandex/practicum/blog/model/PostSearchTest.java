package ru.yandex.practicum.blog.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PostSearchTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "#"})
    void emptyQueryHasNoFilters(String query) {
        PostSearch search = PostSearch.parse(query);

        assertThat(search.hasTitle()).isFalse();
        assertThat(search.hasTags()).isFalse();
    }

    @Test
    void wordsWithoutHashFormTitle() {
        PostSearch search = PostSearch.parse("  Spring   Framework ");

        assertThat(search.title()).isEqualTo("Spring Framework");
        assertThat(search.hasTags()).isFalse();
    }

    @Test
    void wordsWithHashAreTags() {
        PostSearch search = PostSearch.parse("#java #spring #java");

        assertThat(search.hasTitle()).isFalse();
        assertThat(search.tags()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void titleAndTagsCanBeMixed() {
        PostSearch search = PostSearch.parse("#java Spring #web MVC");

        assertThat(search.title()).isEqualTo("Spring MVC");
        assertThat(search.tags()).containsExactlyInAnyOrder("java", "web");
    }
}
