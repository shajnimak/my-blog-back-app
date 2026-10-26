package ru.yandex.practicum.blog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.blog.dto.CommentDto;
import ru.yandex.practicum.blog.dto.NewCommentDto;
import ru.yandex.practicum.blog.service.CommentService;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private static final Pattern NUMERIC_ID = Pattern.compile("\\d{1,18}");

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Фронтенд запрашивает комментарии ещё до загрузки поста, подставляя в адрес "undefined".
     * Ошибка в ответ на такой запрос сбрасывает на странице уже загруженный пост,
     * поэтому для нечислового id возвращается пустой список.
     */
    @GetMapping
    public List<CommentDto> getComments(@PathVariable String postId) {
        if (!NUMERIC_ID.matcher(postId).matches()) {
            return List.of();
        }
        return commentService.getComments(Long.parseLong(postId));
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable long postId, @PathVariable long commentId) {
        return commentService.getComment(postId, commentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable long postId, @RequestBody NewCommentDto comment) {
        return commentService.createComment(postId, comment.text());
    }

    @PutMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable long commentId, @RequestBody CommentDto comment) {
        return commentService.updateComment(commentId, comment.text());
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable long postId, @PathVariable long commentId) {
        commentService.deleteComment(postId, commentId);
    }
}
