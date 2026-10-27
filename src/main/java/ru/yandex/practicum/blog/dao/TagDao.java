package ru.yandex.practicum.blog.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagDao {

    List<String> findByPostId(long postId);

    Map<Long, List<String>> findByPostIds(Collection<Long> postIds);

    void replacePostTags(long postId, Collection<String> tags);
}
