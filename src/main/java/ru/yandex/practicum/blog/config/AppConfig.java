package ru.yandex.practicum.blog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"ru.yandex.practicum.blog.service", "ru.yandex.practicum.blog.dao"})
public class AppConfig {
}
