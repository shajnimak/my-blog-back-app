package ru.yandex.practicum.blog;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.config.AppConfig;
import ru.yandex.practicum.blog.config.WebConfig;

/**
 * Базовый класс интеграционных тестов (DAO и MVC).
 * Конфигурация одинакова для всех наследников, поэтому контекст поднимается один раз
 * и переиспользуется из кеша TestContext Framework. Иерархия контекстов повторяет web.xml.
 * Каждый тест выполняется в транзакции, которая откатывается после теста.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration(name = "root", classes = AppConfig.class),
        @ContextConfiguration(name = "web", classes = WebConfig.class)
})
@TestPropertySource("classpath:test-application.properties")
@Transactional
public abstract class AbstractIntegrationTest {
}
