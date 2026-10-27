package ru.yandex.practicum.blog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.dao.CommentDao;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.dto.CommentDto;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.exception.ValidationException;
import ru.yandex.practicum.blog.mapper.CommentMapper;
import ru.yandex.practicum.blog.model.Comment;

import java.util.List;

@Service
public class CommentService {

    private final CommentDao commentDao;
    private final PostDao postDao;

    public CommentService(CommentDao commentDao, PostDao postDao) {
        this.commentDao = commentDao;
        this.postDao = postDao;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(long postId) {
        requirePostExists(postId);
        return commentDao.findByPostId(postId).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommentDto getComment(long postId, long commentId) {
        return CommentMapper.toDto(loadPostComment(postId, commentId));
    }

    @Transactional
    public CommentDto createComment(long postId, String text) {
        validate(text);
        requirePostExists(postId);
        return CommentMapper.toDto(commentDao.create(postId, text));
    }

    @Transactional
    public CommentDto updateComment(long commentId, String text) {
        validate(text);
        Comment comment = commentDao.findById(commentId).orElseThrow(() -> NotFoundException.comment(commentId));
        commentDao.update(commentId, text);
        return CommentMapper.toDto(new Comment(commentId, text, comment.postId()));
    }

    @Transactional
    public void deleteComment(long postId, long commentId) {
        loadPostComment(postId, commentId);
        commentDao.deleteById(commentId);
    }

    private Comment loadPostComment(long postId, long commentId) {
        return commentDao.findById(commentId)
                .filter(comment -> comment.postId() == postId)
                .orElseThrow(() -> NotFoundException.comment(commentId));
    }

    private void requirePostExists(long postId) {
        if (!postDao.existsById(postId)) {
            throw NotFoundException.post(postId);
        }
    }

    private static void validate(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Текст комментария не может быть пустым");
        }
    }
}
