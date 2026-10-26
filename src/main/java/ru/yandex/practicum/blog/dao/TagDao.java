package ru.yandex.practicum.blog.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagDao {

    List<String> findByPostId(long postId);

    /**
     * @return теги, сгруппированные по id поста; посты без тегов в результат не попадают
     */
    Map<Long, List<String>> findByPostIds(Collection<Long> postIds);

    /**
     * Заменяет теги поста на переданные. Отсутствующие в справочнике теги создаются.
     */
    void replacePostTags(long postId, Collection<String> tags);
}
