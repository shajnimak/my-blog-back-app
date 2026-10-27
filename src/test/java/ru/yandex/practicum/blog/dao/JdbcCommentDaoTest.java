package ru.yandex.practicum.blog.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.blog.model.Comment;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcCommentDaoTest extends AbstractDaoTest {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private PostDao postDao;

    private long postId;

    @BeforeEach
    void createPost() {
        postId = postDao.create("Пост", "текст");
    }

    @Test
    void createAndFindById() {
        Comment created = commentDao.create(postId, "Комментарий");

        assertThat(created.id()).isNotNull();
        assertThat(commentDao.findById(created.id())).contains(new Comment(created.id(), "Комментарий", postId));
    }

    @Test
    void findByPostIdReturnsCommentsInCreationOrder() {
        long otherPostId = postDao.create("Другой пост", "текст");
        Comment first = commentDao.create(postId, "Первый");
        Comment second = commentDao.create(postId, "Второй");
        commentDao.create(otherPostId, "Чужой");

        assertThat(commentDao.findByPostId(postId)).containsExactly(first, second);
    }

    @Test
    void updateChangesText() {
        Comment comment = commentDao.create(postId, "Старый текст");

        assertThat(commentDao.update(comment.id(), "Новый текст")).isTrue();
        assertThat(commentDao.findById(comment.id()))
                .hasValueSatisfying(updated -> assertThat(updated.text()).isEqualTo("Новый текст"));
    }

    @Test
    void deleteRemovesComment() {
        Comment comment = commentDao.create(postId, "Комментарий");

        assertThat(commentDao.deleteById(comment.id())).isTrue();
        assertThat(commentDao.findById(comment.id())).isEmpty();
    }

    @Test
    void operationsOnUnknownCommentReturnNothing() {
        assertThat(commentDao.findById(-1)).isEmpty();
        assertThat(commentDao.update(-1, "текст")).isFalse();
        assertThat(commentDao.deleteById(-1)).isFalse();
    }
}
