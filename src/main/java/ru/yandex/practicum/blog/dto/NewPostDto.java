package ru.yandex.practicum.blog.dto;

import java.util.List;

public record NewPostDto(String title, String text, List<String> tags) {
}
