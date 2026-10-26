package ru.yandex.practicum.blog.service;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.blog.dao.CommentDao;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.dao.TagDao;

/**
 * Базовый класс тестов слоя сервисов: в контексте только сервисы, DAO заменены моками.
 * Набор бинов и моков общий для всех наследников, поэтому контекст кешируется и создаётся один раз.
 * Моки сбрасываются после каждого теста.
 */
@SpringJUnitConfig(classes = {PostService.class, CommentService.class})
abstract class AbstractServiceTest {

    @MockitoBean
    protected PostDao postDao;

    @MockitoBean
    protected TagDao tagDao;

    @MockitoBean
    protected CommentDao commentDao;
}
