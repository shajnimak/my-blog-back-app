package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.Comment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcCommentDao implements CommentDao {

    private static final RowMapper<Comment> COMMENT_ROW_MAPPER = (rs, rowNum) -> new Comment(
            rs.getLong("id"),
            rs.getString("text"),
            rs.getLong("post_id")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCommentDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Comment> findByPostId(long postId) {
        return jdbcTemplate.query("SELECT id, text, post_id FROM comments WHERE post_id = :postId ORDER BY id",
                new MapSqlParameterSource("postId", postId), COMMENT_ROW_MAPPER);
    }

    @Override
    public Optional<Comment> findById(long id) {
        return jdbcTemplate.query("SELECT id, text, post_id FROM comments WHERE id = :id",
                        new MapSqlParameterSource("id", id), COMMENT_ROW_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public Comment create(long postId, String text) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("text", text);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO comments (post_id, text) VALUES (:postId, :text)", params, keyHolder, new String[]{"id"});
        long id = Objects.requireNonNull(keyHolder.getKey(), "Не получен id созданного комментария").longValue();
        return new Comment(id, text, postId);
    }

    @Override
    public boolean update(long id, String text) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("text", text);
        return jdbcTemplate.update("UPDATE comments SET text = :text WHERE id = :id", params) > 0;
    }

    @Override
    public boolean deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM comments WHERE id = :id", new MapSqlParameterSource("id", id)) > 0;
    }
}
