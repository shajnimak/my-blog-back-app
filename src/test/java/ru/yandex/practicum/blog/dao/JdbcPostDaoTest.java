package ru.yandex.practicum.blog.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.blog.AbstractIntegrationTest;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearch;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostDaoTest extends AbstractIntegrationTest {

    @Autowired
    private PostDao postDao;

    @Autowired
    private TagDao tagDao;

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createAndFindById() {
        long id = postDao.create("Первый пост", "Текст поста");

        assertThat(postDao.findById(id)).hasValueSatisfying(post -> {
            assertThat(post.id()).isEqualTo(id);
            assertThat(post.title()).isEqualTo("Первый пост");
            assertThat(post.text()).isEqualTo("Текст поста");
            assertThat(post.likesCount()).isZero();
            assertThat(post.commentsCount()).isZero();
        });
    }

    @Test
    void findByIdReturnsEmptyForUnknownPost() {
        assertThat(postDao.findById(-1)).isEmpty();
        assertThat(postDao.existsById(-1)).isFalse();
    }

    @Test
    void findAllReturnsNewestPostsFirstWithPagination() {
        long first = postDao.create("Пост 1", "текст");
        long second = postDao.create("Пост 2", "текст");
        long third = postDao.create("Пост 3", "текст");

        PostSearch noFilter = PostSearch.parse("");
        assertThat(postDao.findAll(noFilter, 2, 0)).extracting(Post::id).containsExactly(third, second);
        assertThat(postDao.findAll(noFilter, 2, 2)).extracting(Post::id).containsExactly(first);
        assertThat(postDao.count(noFilter)).isEqualTo(3);
    }

    @Test
    void findAllFiltersByTitleIgnoringCase() {
        long spring = postDao.create("Изучаем Spring Framework", "текст");
        postDao.create("Рецепт борща", "текст");

        PostSearch search = PostSearch.parse("spring");
        assertThat(postDao.findAll(search, 10, 0)).extracting(Post::id).containsExactly(spring);
        assertThat(postDao.count(search)).isEqualTo(1);
    }

    @Test
    void findAllTreatsLikeWildcardsAsPlainText() {
        long percent = postDao.create("Скидка 50%", "текст");
        postDao.create("Скидка 500 рублей", "текст");

        assertThat(postDao.findAll(PostSearch.parse("50%"), 10, 0)).extracting(Post::id).containsExactly(percent);
    }

    @Test
    void findAllReturnsOnlyPostsWithAllRequestedTags() {
        long javaAndSpring = postDao.create("Пост 1", "текст");
        long javaOnly = postDao.create("Пост 2", "текст");
        postDao.create("Пост 3", "текст");
        tagDao.replacePostTags(javaAndSpring, List.of("java", "spring"));
        tagDao.replacePostTags(javaOnly, List.of("java"));

        assertThat(postDao.findAll(PostSearch.parse("#java"), 10, 0))
                .extracting(Post::id).containsExactly(javaOnly, javaAndSpring);
        assertThat(postDao.findAll(PostSearch.parse("#java #spring"), 10, 0))
                .extracting(Post::id).containsExactly(javaAndSpring);
        assertThat(postDao.count(PostSearch.parse("#java #spring"))).isEqualTo(1);
    }

    @Test
    void findAllCombinesTitleAndTags() {
        long matching = postDao.create("Spring MVC", "текст");
        long otherTitle = postDao.create("Hibernate", "текст");
        tagDao.replacePostTags(matching, List.of("java"));
        tagDao.replacePostTags(otherTitle, List.of("java"));

        PostSearch search = new PostSearch("mvc", Set.of("java"));
        assertThat(postDao.findAll(search, 10, 0)).extracting(Post::id).containsExactly(matching);
    }

    @Test
    void findAllCountsComments() {
        long id = postDao.create("Пост", "текст");
        commentDao.create(id, "Комментарий 1");
        commentDao.create(id, "Комментарий 2");

        assertThat(postDao.findAll(PostSearch.parse(null), 10, 0))
                .singleElement()
                .extracting(Post::commentsCount)
                .isEqualTo(2);
    }

    @Test
    void updateChangesTitleAndText() {
        long id = postDao.create("Старое название", "Старый текст");

        assertThat(postDao.update(id, "Новое название", "Новый текст")).isTrue();
        assertThat(postDao.findById(id)).hasValueSatisfying(post -> {
            assertThat(post.title()).isEqualTo("Новое название");
            assertThat(post.text()).isEqualTo("Новый текст");
        });
    }

    @Test
    void updateReturnsFalseForUnknownPost() {
        assertThat(postDao.update(-1, "Название", "Текст")).isFalse();
    }

    @Test
    void deleteRemovesPostWithCommentsAndTagLinks() {
        long id = postDao.create("Пост", "текст");
        commentDao.create(id, "Комментарий");
        tagDao.replacePostTags(id, List.of("java"));

        assertThat(postDao.deleteById(id)).isTrue();

        assertThat(postDao.existsById(id)).isFalse();
        assertThat(commentDao.findByPostId(id)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post_tags WHERE post_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void incrementLikesReturnsNewValue() {
        long id = postDao.create("Пост", "текст");

        assertThat(postDao.incrementLikes(id)).contains(1);
        assertThat(postDao.incrementLikes(id)).contains(2);
        assertThat(postDao.findById(id)).hasValueSatisfying(post -> assertThat(post.likesCount()).isEqualTo(2));
    }

    @Test
    void incrementLikesReturnsEmptyForUnknownPost() {
        assertThat(postDao.incrementLikes(-1)).isEmpty();
    }

    @Test
    void updateAndFindImage() {
        long id = postDao.create("Пост", "текст");
        byte[] image = {1, 2, 3};

        assertThat(postDao.findImage(id)).isEmpty();
        assertThat(postDao.updateImage(id, image)).isTrue();
        assertThat(postDao.findImage(id)).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(1, 2, 3));
    }

    @Test
    void updateImageReturnsFalseForUnknownPost() {
        assertThat(postDao.updateImage(-1, new byte[]{1})).isFalse();
    }
}
