package ru.yandex.practicum.blog.mapper;

import ru.yandex.practicum.blog.dto.PostDto;
import ru.yandex.practicum.blog.model.Post;

public final class PostMapper {

    public static final int PREVIEW_LENGTH = 128;
    private static final String ELLIPSIS = "…";

    private PostMapper() {
    }

    public static PostDto toDto(Post post) {
        return toDto(post, post.text());
    }

    public static PostDto toPreviewDto(Post post) {
        return toDto(post, preview(post.text()));
    }

    static String preview(String text) {
        if (text == null || text.length() <= PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_LENGTH) + ELLIPSIS;
    }

    private static PostDto toDto(Post post, String text) {
        return new PostDto(post.id(), post.title(), text, post.tags(), post.likesCount(), post.commentsCount());
    }
}
