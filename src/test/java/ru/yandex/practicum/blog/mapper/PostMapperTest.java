package ru.yandex.practicum.blog.mapper;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.blog.dto.PostDto;
import ru.yandex.practicum.blog.model.Post;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostMapperTest {

    @Test
    void previewKeepsTextUpTo128Characters() {
        String text = "x".repeat(PostMapper.PREVIEW_LENGTH);

        assertThat(PostMapper.preview(text)).isEqualTo(text);
    }

    @Test
    void previewCutsLongTextAndAddsEllipsis() {
        String text = "x".repeat(PostMapper.PREVIEW_LENGTH + 1);

        assertThat(PostMapper.preview(text)).isEqualTo("x".repeat(PostMapper.PREVIEW_LENGTH) + "…");
    }

    @Test
    void toDtoKeepsFullText() {
        Post post = new Post(1L, "Пост", "y".repeat(500), List.of("tag"), 2, 3);

        assertThat(PostMapper.toDto(post)).isEqualTo(new PostDto(1L, "Пост", "y".repeat(500), List.of("tag"), 2, 3));
    }
}
