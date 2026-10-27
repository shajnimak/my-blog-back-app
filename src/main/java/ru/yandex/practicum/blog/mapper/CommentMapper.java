package ru.yandex.practicum.blog.mapper;

import ru.yandex.practicum.blog.dto.CommentDto;
import ru.yandex.practicum.blog.model.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(comment.id(), comment.text(), comment.postId());
    }
}
