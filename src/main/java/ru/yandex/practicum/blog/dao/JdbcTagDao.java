package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class JdbcTagDao implements TagDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTagDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findByPostId(long postId) {
        return jdbcTemplate.queryForList("""
                SELECT t.name
                FROM tags t
                         JOIN post_tags pt ON pt.tag_id = t.id
                WHERE pt.post_id = :postId
                ORDER BY t.name""", new MapSqlParameterSource("postId", postId), String.class);
    }

    @Override
    public Map<Long, List<String>> findByPostIds(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> tagsByPostId = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT pt.post_id, t.name
                        FROM post_tags pt
                                 JOIN tags t ON t.id = pt.tag_id
                        WHERE pt.post_id IN (:postIds)
                        ORDER BY t.name""",
                new MapSqlParameterSource("postIds", postIds),
                rs -> {
                    tagsByPostId.computeIfAbsent(rs.getLong("post_id"), id -> new ArrayList<>())
                            .add(rs.getString("name"));
                });
        return tagsByPostId;
    }

    @Override
    public void replacePostTags(long postId, Collection<String> tags) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = :postId", new MapSqlParameterSource("postId", postId));
        if (tags.isEmpty()) {
            return;
        }

        Set<String> names = new LinkedHashSet<>(tags);
        createMissingTags(names);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("names", names);
        jdbcTemplate.update("""
                INSERT INTO post_tags (post_id, tag_id)
                SELECT :postId, id FROM tags WHERE name IN (:names)""", params);
    }

    private void createMissingTags(Set<String> names) {
        List<String> existing = jdbcTemplate.queryForList(
                "SELECT name FROM tags WHERE name IN (:names)", new MapSqlParameterSource("names", names), String.class);

        MapSqlParameterSource[] batch = names.stream()
                .filter(name -> !existing.contains(name))
                .map(name -> new MapSqlParameterSource("name", name))
                .toArray(MapSqlParameterSource[]::new);
        if (batch.length > 0) {
            jdbcTemplate.batchUpdate("INSERT INTO tags (name) VALUES (:name)", batch);
        }
    }
}
