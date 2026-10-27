package ru.yandex.practicum.blog.dao;

import ru.yandex.practicum.blog.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentDao {

    List<Comment> findByPostId(long postId);

    Optional<Comment> findById(long id);

    Comment create(long postId, String text);

    boolean update(long id, String text);

    boolean deleteById(long id);
}
