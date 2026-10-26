package ru.yandex.practicum.blog.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException post(long id) {
        return new NotFoundException("Пост с id=" + id + " не найден");
    }

    public static NotFoundException comment(long id) {
        return new NotFoundException("Комментарий с id=" + id + " не найден");
    }
}
