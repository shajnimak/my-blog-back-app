package ru.yandex.practicum.blog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"ru.yandex.practicum.blog.service", "ru.yandex.practicum.blog.dao"})
@Import(DataSourceConfig.class)
public class AppConfig {
}
