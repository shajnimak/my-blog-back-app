package ru.yandex.practicum.blog.dao;

import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearch;

import java.util.List;
import java.util.Optional;

public interface PostDao {

    List<Post> findAll(PostSearch search, int limit, int offset);

    int count(PostSearch search);

    Optional<Post> findById(long id);

    boolean existsById(long id);

    long create(String title, String text);

    boolean update(long id, String title, String text);

    boolean deleteById(long id);

    Optional<Integer> incrementLikes(long id);

    boolean updateImage(long id, byte[] image);

    Optional<byte[]> findImage(long id);
}
