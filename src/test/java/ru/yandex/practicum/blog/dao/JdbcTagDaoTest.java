package ru.yandex.practicum.blog.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.blog.AbstractIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class JdbcTagDaoTest extends AbstractIntegrationTest {

    @Autowired
    private TagDao tagDao;

    @Autowired
    private PostDao postDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void replacePostTagsSavesTagsSortedByName() {
        long postId = postDao.create("Пост", "текст");

        tagDao.replacePostTags(postId, List.of("spring", "java"));

        assertThat(tagDao.findByPostId(postId)).containsExactly("java", "spring");
    }

    @Test
    void replacePostTagsRemovesOldTags() {
        long postId = postDao.create("Пост", "текст");
        tagDao.replacePostTags(postId, List.of("java", "spring"));

        tagDao.replacePostTags(postId, List.of("kotlin"));
        assertThat(tagDao.findByPostId(postId)).containsExactly("kotlin");

        tagDao.replacePostTags(postId, List.of());
        assertThat(tagDao.findByPostId(postId)).isEmpty();
    }

    @Test
    void replacePostTagsReusesExistingTags() {
        long first = postDao.create("Пост 1", "текст");
        long second = postDao.create("Пост 2", "текст");

        tagDao.replacePostTags(first, List.of("java"));
        tagDao.replacePostTags(second, List.of("java", "java"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags WHERE name = 'java'", Integer.class))
                .isEqualTo(1);
        assertThat(tagDao.findByPostId(second)).containsExactly("java");
    }

    @Test
    void findByPostIdsGroupsTagsByPost() {
        long first = postDao.create("Пост 1", "текст");
        long second = postDao.create("Пост 2", "текст");
        long withoutTags = postDao.create("Пост 3", "текст");
        tagDao.replacePostTags(first, List.of("java", "spring"));
        tagDao.replacePostTags(second, List.of("java"));

        assertThat(tagDao.findByPostIds(List.of(first, second, withoutTags))).containsOnly(
                entry(first, List.of("java", "spring")),
                entry(second, List.of("java"))
        );
    }

    @Test
    void findByPostIdsReturnsEmptyMapForEmptyInput() {
        assertThat(tagDao.findByPostIds(List.of())).isEmpty();
    }
}
