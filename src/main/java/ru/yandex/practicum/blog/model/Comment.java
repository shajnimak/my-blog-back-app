package ru.yandex.practicum.blog.model;

public record Comment(Long id, String text, Long postId) {
}
