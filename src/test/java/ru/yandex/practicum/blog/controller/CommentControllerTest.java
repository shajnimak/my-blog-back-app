package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.blog.AbstractIntegrationTest;
import ru.yandex.practicum.blog.dao.CommentDao;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.model.Comment;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostDao postDao;

    @Autowired
    private CommentDao commentDao;

    private long postId;

    @BeforeEach
    void createPost() {
        postId = postDao.create("Пост", "текст");
    }

    @Test
    void getComments() throws Exception {
        Comment first = commentDao.create(postId, "Комментарий к посту");
        commentDao.create(postId, "Ещё один комментарий");

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(first.id()))
                .andExpect(jsonPath("$[0].text").value("Комментарий к посту"))
                .andExpect(jsonPath("$[0].postId").value(postId))
                .andExpect(jsonPath("$[1].text").value("Ещё один комментарий"));
    }

    @Test
    void getCommentsForUndefinedPostIdReturnsEmptyList() throws Exception {
        // фронтенд запрашивает комментарии до загрузки поста
        mockMvc.perform(get("/api/posts/undefined/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCommentsForUnknownPostReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/{postId}/comments", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getComment() throws Exception {
        Comment comment = commentDao.create(postId, "Комментарий");

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, comment.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment.id()))
                .andExpect(jsonPath("$.text").value("Комментарий"))
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    void getCommentOfAnotherPostReturnsNotFound() throws Exception {
        long otherPostId = postDao.create("Другой пост", "текст");
        Comment comment = commentDao.create(otherPostId, "Комментарий");

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, comment.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComment() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Новый комментарий", "postId": %d}
                                """.formatted(postId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.text").value("Новый комментарий"))
                .andExpect(jsonPath("$.postId").value(postId));

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.commentsCount").value(1));
    }

    @Test
    void createCommentForUnknownPostReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", -1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Комментарий", "postId": -1}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateComment() throws Exception {
        Comment comment = commentDao.create(postId, "Старый текст");

        mockMvc.perform(put("/api/posts/{postId}/comments/{id}", postId, comment.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": %d, "text": "Новый текст", "postId": %d}
                                """.formatted(comment.id(), postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment.id()))
                .andExpect(jsonPath("$.text").value("Новый текст"))
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    void updateCommentWorksWithFrontendUrl() throws Exception {
        // фронтенд подставляет id комментария вместо id поста
        Comment comment = commentDao.create(postId, "Старый текст");

        mockMvc.perform(put("/api/posts/{id}/comments/{id}", comment.id(), comment.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": %d, "text": "Новый текст", "postId": %d}
                                """.formatted(comment.id(), postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Новый текст"))
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    void deleteComment() throws Exception {
        Comment comment = commentDao.create(postId, "Комментарий");

        mockMvc.perform(delete("/api/posts/{postId}/comments/{id}", postId, comment.id()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, comment.id()))
                .andExpect(status().isNotFound());
    }
}
