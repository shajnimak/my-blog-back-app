package ru.yandex.practicum.blog.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PostSearch(String title, Set<String> tags) {

    private static final String TAG_PREFIX = "#";

    public PostSearch {
        title = title == null || title.isBlank() ? null : title;
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static PostSearch parse(String query) {
        if (query == null || query.isBlank()) {
            return new PostSearch(null, Set.of());
        }

        List<String> titleWords = new ArrayList<>();
        Set<String> tags = new LinkedHashSet<>();
        for (String word : query.trim().split("\\s+")) {
            if (word.startsWith(TAG_PREFIX)) {
                String tag = word.substring(TAG_PREFIX.length());
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            } else {
                titleWords.add(word);
            }
        }
        return new PostSearch(String.join(" ", titleWords), tags);
    }

    public boolean hasTitle() {
        return title != null;
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }
}
