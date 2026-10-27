package ru.yandex.practicum.blog.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.blog.dao.CommentDao;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.dao.TagDao;

@SpringBootTest(classes = {PostService.class, CommentService.class})
abstract class AbstractServiceTest {

    @MockitoBean
    protected PostDao postDao;

    @MockitoBean
    protected TagDao tagDao;

    @MockitoBean
    protected CommentDao commentDao;
}
