package ru.yandex.practicum.blog.dao;

import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest
@Import({JdbcPostDao.class, JdbcTagDao.class, JdbcCommentDao.class})
abstract class AbstractDaoTest {
}
