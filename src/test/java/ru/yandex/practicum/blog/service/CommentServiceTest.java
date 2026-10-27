package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.blog.dto.CommentDto;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.exception.ValidationException;
import ru.yandex.practicum.blog.model.Comment;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentServiceTest extends AbstractServiceTest {

    @Autowired
    private CommentService commentService;

    @Test
    void getCommentsReturnsPostComments() {
        when(postDao.existsById(1L)).thenReturn(true);
        when(commentDao.findByPostId(1L)).thenReturn(List.of(
                new Comment(1L, "Первый", 1L),
                new Comment(2L, "Второй", 1L)
        ));

        assertThat(commentService.getComments(1L)).containsExactly(
                new CommentDto(1L, "Первый", 1L),
                new CommentDto(2L, "Второй", 1L)
        );
    }

    @Test
    void getCommentsThrowsWhenPostNotFound() {
        when(postDao.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(1L)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(commentDao);
    }

    @Test
    void getComment() {
        when(commentDao.findById(2L)).thenReturn(Optional.of(new Comment(2L, "Комментарий", 1L)));

        assertThat(commentService.getComment(1L, 2L)).isEqualTo(new CommentDto(2L, "Комментарий", 1L));
    }

    @Test
    void getCommentOfAnotherPostIsNotFound() {
        when(commentDao.findById(2L)).thenReturn(Optional.of(new Comment(2L, "Комментарий", 5L)));

        assertThatThrownBy(() -> commentService.getComment(1L, 2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createComment() {
        when(postDao.existsById(1L)).thenReturn(true);
        when(commentDao.create(1L, "Новый")).thenReturn(new Comment(3L, "Новый", 1L));

        assertThat(commentService.createComment(1L, "Новый")).isEqualTo(new CommentDto(3L, "Новый", 1L));
    }

    @Test
    void createCommentThrowsWhenPostNotFound() {
        when(postDao.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.createComment(1L, "Новый")).isInstanceOf(NotFoundException.class);
        verify(commentDao, never()).create(anyLong(), anyString());
    }

    @Test
    void createCommentRejectsBlankText() {
        assertThatThrownBy(() -> commentService.createComment(1L, "  ")).isInstanceOf(ValidationException.class);
        verifyNoInteractions(postDao, commentDao);
    }

    @Test
    void updateCommentKeepsPostId() {
        when(commentDao.findById(2L)).thenReturn(Optional.of(new Comment(2L, "Старый", 1L)));
        when(commentDao.update(2L, "Новый")).thenReturn(true);

        assertThat(commentService.updateComment(2L, "Новый")).isEqualTo(new CommentDto(2L, "Новый", 1L));
        verify(commentDao).update(2L, "Новый");
    }

    @Test
    void updateCommentThrowsWhenNotFound() {
        when(commentDao.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(2L, "Новый")).isInstanceOf(NotFoundException.class);
        verify(commentDao, never()).update(anyLong(), anyString());
    }

    @Test
    void deleteComment() {
        when(commentDao.findById(2L)).thenReturn(Optional.of(new Comment(2L, "Комментарий", 1L)));

        commentService.deleteComment(1L, 2L);

        verify(commentDao).deleteById(2L);
    }

    @Test
    void deleteCommentOfAnotherPostIsNotFound() {
        when(commentDao.findById(2L)).thenReturn(Optional.of(new Comment(2L, "Комментарий", 5L)));

        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L)).isInstanceOf(NotFoundException.class);
        verify(commentDao, never()).deleteById(anyLong());
    }
}
