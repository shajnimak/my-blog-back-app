package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcPostDao implements PostDao {

    private static final String SELECT_POSTS = """
            SELECT p.id, p.title, p.text, p.likes_count,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count
            FROM posts p""";

    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> new Post(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("text"),
            List.of(),
            rs.getInt("likes_count"),
            rs.getInt("comments_count")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPostDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Post> findAll(PostSearch search, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        String sql = SELECT_POSTS + whereClause(search, params) + " ORDER BY p.id DESC LIMIT :limit OFFSET :offset";
        return jdbcTemplate.query(sql, params, POST_ROW_MAPPER);
    }

    @Override
    public int count(PostSearch search) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts p" + whereClause(search, params), params, Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<Post> findById(long id) {
        return jdbcTemplate.query(SELECT_POSTS + " WHERE p.id = :id", new MapSqlParameterSource("id", id), POST_ROW_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsById(long id) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM posts WHERE id = :id)", new MapSqlParameterSource("id", id), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public long create(String title, String text) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", title)
                .addValue("text", text);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO posts (title, text) VALUES (:title, :text)", params, keyHolder, new String[]{"id"});
        return Objects.requireNonNull(keyHolder.getKey(), "Не получен id созданного поста").longValue();
    }

    @Override
    public boolean update(long id, String title, String text) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", title)
                .addValue("text", text);
        return jdbcTemplate.update("UPDATE posts SET title = :title, text = :text WHERE id = :id", params) > 0;
    }

    @Override
    public boolean deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM posts WHERE id = :id", new MapSqlParameterSource("id", id)) > 0;
    }

    @Override
    public Optional<Integer> incrementLikes(long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        if (jdbcTemplate.update("UPDATE posts SET likes_count = likes_count + 1 WHERE id = :id", params) == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                jdbcTemplate.queryForObject("SELECT likes_count FROM posts WHERE id = :id", params, Integer.class));
    }

    @Override
    public boolean updateImage(long id, byte[] image) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("image", image);
        return jdbcTemplate.update("UPDATE posts SET image = :image WHERE id = :id", params) > 0;
    }

    @Override
    public Optional<byte[]> findImage(long id) {
        return jdbcTemplate.query("SELECT image FROM posts WHERE id = :id", new MapSqlParameterSource("id", id),
                        (rs, rowNum) -> rs.getBytes("image"))
                .stream()
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static String whereClause(PostSearch search, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (search.hasTitle()) {
            conditions.add("LOWER(p.title) LIKE :title ESCAPE '\\'");
            params.addValue("title", "%" + escapeLike(search.title().toLowerCase(Locale.ROOT)) + "%");
        }
        if (search.hasTags()) {
            // пост должен содержать все теги из запроса
            conditions.add("""
                    p.id IN (SELECT pt.post_id
                             FROM post_tags pt
                                      JOIN tags t ON t.id = pt.tag_id
                             WHERE t.name IN (:tags)
                             GROUP BY pt.post_id
                             HAVING COUNT(*) = :tagsCount)""");
            params.addValue("tags", search.tags());
            params.addValue("tagsCount", search.tags().size());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
