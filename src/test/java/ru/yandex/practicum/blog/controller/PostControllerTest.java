package ru.yandex.practicum.blog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.blog.AbstractIntegrationTest;
import ru.yandex.practicum.blog.dao.CommentDao;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.dao.TagDao;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostDao postDao;

    @Autowired
    private TagDao tagDao;

    @Autowired
    private CommentDao commentDao;

    @Test
    void createPost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Новый пост", "text": "Текст поста", "tags": ["tag_1", "tag_2"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Новый пост"))
                .andExpect(jsonPath("$.text").value("Текст поста"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("tag_1", "tag_2")))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void createPostWithBlankTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "text": "Текст", "tags": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getPostsReturnsPageWithPreview() throws Exception {
        long first = postDao.create("Пост 1", "a".repeat(200));
        postDao.create("Пост 2", "текст");
        long third = postDao.create("Пост 3", "текст");
        tagDao.replacePostTags(third, List.of("java"));
        commentDao.create(first, "Комментарий");

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.posts[0].id").value(third))
                .andExpect(jsonPath("$.posts[0].tags[0]").value("java"))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.lastPage").value(2));

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].id").value(first))
                .andExpect(jsonPath("$.posts[0].text").value("a".repeat(128) + "…"))
                .andExpect(jsonPath("$.posts[0].commentsCount").value(1))
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getPostsFiltersByTitleAndTags() throws Exception {
        long springPost = postDao.create("Spring MVC", "текст");
        long otherPost = postDao.create("Spring Data", "текст");
        postDao.create("Рецепт", "текст");
        tagDao.replacePostTags(springPost, List.of("java", "web"));
        tagDao.replacePostTags(otherPost, List.of("java"));

        mockMvc.perform(get("/api/posts")
                        .param("search", "spring #java #web")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].id").value(springPost))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getPostsWithoutRequiredParamsReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts").param("search", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostReturnsFullText() throws Exception {
        long id = postDao.create("Пост", "b".repeat(300));

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.text").value("b".repeat(300)))
                .andExpect(jsonPath("$.tags", hasSize(0)));
    }

    @Test
    void getUnknownPostReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", -1))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePost() throws Exception {
        long id = postDao.create("Старое название", "Старый текст");
        tagDao.replacePostTags(id, List.of("old"));
        postDao.incrementLikes(id);

        mockMvc.perform(put("/api/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": %d, "title": "Новое название", "text": "Новый текст", "tags": ["new"]}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.text").value("Новый текст"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("new")))
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void deletePostRemovesItWithComments() throws Exception {
        long id = postDao.create("Пост", "текст");
        commentDao.create(id, "Комментарий");

        mockMvc.perform(delete("/api/posts/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/posts/{id}/comments", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLikeReturnsUpdatedCount() throws Exception {
        long id = postDao.create("Пост", "текст");

        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void uploadAndDownloadImage() throws Exception {
        long id = postDao.create("Пост", "текст");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/{id}/image", id)
                        .file(new MockMultipartFile("image", "image.png", MediaType.IMAGE_PNG_VALUE, png)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}/image", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void getMissingImageReturnsNotFound() throws Exception {
        long id = postDao.create("Пост", "текст");

        mockMvc.perform(get("/api/posts/{id}/image", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("http://localhost:8080/api/posts/1")
                        .header(HttpHeaders.ORIGIN, "http://localhost")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }
}
