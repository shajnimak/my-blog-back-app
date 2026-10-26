package ru.yandex.practicum.blog.dto;

import java.util.List;

public record PostPageDto(
        List<PostDto> posts,
        boolean hasPrev,
        boolean hasNext,
        int lastPage
) {
}
