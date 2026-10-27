package ru.yandex.practicum.blog.model;

import java.util.List;

public record Post(
        Long id,
        String title,
        String text,
        List<String> tags,
        int likesCount,
        int commentsCount
) {

    public Post {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public Post withTags(List<String> tags) {
        return new Post(id, title, text, tags, likesCount, commentsCount);
    }
}
