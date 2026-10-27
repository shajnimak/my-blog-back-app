# my-blog-back-app

Бэкенд веб-приложения «Блог». Отдаёт фронтенду REST API для работы с постами, тегами, лайками,
картинками и комментариями. Приложение собирается в исполняемый jar и запускается со встроенным Tomcat.

## Стек

- Java 21
- Spring Boot 3.5 (Spring Web MVC, Spring Data JDBC / JdbcTemplate)
- H2 (файловая БД для приложения, in-memory для тестов)
- Gradle (Gradle Wrapper)
- Встроенный Tomcat
- JUnit 5, Spring Boot Test, Mockito, AssertJ, MockMvc

## Структура проекта

```
src/main/java/ru/yandex/practicum/blog
├── BlogApplication — точка входа Spring Boot
├── config          — WebConfig (CORS)
├── controller      — REST-контроллеры и обработчик ошибок
├── service         — бизнес-логика: пагинация, поиск, теги, комментарии
├── dao             — интерфейсы DAO и их реализации на NamedParameterJdbcTemplate
├── model           — доменные модели (Post, Comment, PostSearch)
├── dto             — объекты запросов и ответов REST API
├── mapper          — преобразование моделей в DTO
└── exception       — исключения слоя сервисов
src/main/resources
├── application.properties — порт, подключение к БД, ограничения на загрузку файлов, логирование
└── schema.sql             — схема БД, накатывается при старте
```

DataSource, `JdbcTemplate`, `NamedParameterJdbcTemplate`, менеджер транзакций, Spring MVC и загрузку файлов
настраивает автоконфигурация Spring Boot.

## Схема БД

```
posts      (id, title, text, likes_count, image)
tags       (id, name UNIQUE)
post_tags  (post_id → posts.id ON DELETE CASCADE, tag_id → tags.id ON DELETE CASCADE, PK(post_id, tag_id))
comments   (id, post_id → posts.id ON DELETE CASCADE, text)
```

Теги вынесены в справочник и связаны с постами отношением «многие ко многим». При удалении поста
его комментарии и связи с тегами удаляются каскадно. Число комментариев не хранится, а считается запросом.

## REST API

| Метод    | Адрес                                            | Описание                                                |
|----------|--------------------------------------------------|---------------------------------------------------------|
| `GET`    | `/api/posts?search=...&pageNumber=1&pageSize=5`  | Лента постов с поиском и пагинацией                     |
| `GET`    | `/api/posts/{id}`                                | Пост целиком                                            |
| `POST`   | `/api/posts`                                     | Создание поста                                          |
| `PUT`    | `/api/posts/{id}`                                | Редактирование поста                                    |
| `DELETE` | `/api/posts/{id}`                                | Удаление поста вместе с комментариями                   |
| `POST`   | `/api/posts/{id}/likes`                          | +1 лайк, в ответе новое число лайков                    |
| `PUT`    | `/api/posts/{id}/image`                          | Загрузка картинки (`multipart/form-data`, поле `image`) |
| `GET`    | `/api/posts/{id}/image`                          | Картинка поста (байты)                                  |
| `GET`    | `/api/posts/{postId}/comments`                   | Комментарии поста                                       |
| `GET`    | `/api/posts/{postId}/comments/{commentId}`       | Комментарий                                             |
| `POST`   | `/api/posts/{postId}/comments`                   | Добавление комментария                                  |
| `PUT`    | `/api/posts/{postId}/comments/{commentId}`       | Редактирование комментария                              |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}`       | Удаление комментария                                    |

**Поиск.** Строка `search` делится на слова по пробелам. Слова, начинающиеся с `#`, — это теги: пост должен
содержать все указанные теги. Остальные слова склеиваются через пробел и ищутся как подстрока в названии поста
без учёта регистра. Например, `spring #java #web` найдёт посты с тегами `java` и `web`, в названии которых есть
«spring». Посты в ленте отсортированы от новых к старым.

**Текст в ленте** длиннее 128 символов обрезается до 128 символов и дополняется «…». На странице поста текст
отдаётся полностью.

**Ошибки.** Если пост или комментарий не найден, возвращается `404`, если запрос некорректный — `400`.
Тело ответа: `{"message": "..."}`.

## Требования

- JDK 21
- Gradle ставить не нужно — в проекте есть Gradle Wrapper (`./gradlew`, на Windows `gradlew.bat`)

## Сборка

```bash
./gradlew clean build
```

Тесты запускаются при сборке. Исполняемый jar — `build/libs/my-blog-back-app-1.0.0-SNAPSHOT.jar`.

## Тесты

```bash
./gradlew test
```

Отчёт — `build/reports/tests/test/index.html`.

| Пакет        | Что проверяется                                | Как                                                          |
|--------------|------------------------------------------------|--------------------------------------------------------------|
| `dao`        | DAO на реальной БД                             | `@JdbcTest` + DAO-бины, встроенная H2 в памяти               |
| `controller` | REST API целиком: контроллер → сервис → DAO → БД | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@AutoConfigureTestDatabase` |
| `service`    | бизнес-логика сервисов                         | `@SpringBootTest(classes = ...)`, DAO заменены `@MockitoBean` |
| `model`, `mapper` | разбор строки поиска, обрезка текста      | JUnit 5 без Spring                                           |

Тесты каждой группы наследуют общий базовый класс (`AbstractDaoTest`, `AbstractIntegrationTest`,
`AbstractServiceTest`), поэтому конфигурация внутри группы одинаковая и Spring создаёт на все тесты только
три контекста, а дальше берёт их из кеша. Тесты DAO и MVC выполняются в транзакции, которая откатывается
после каждого теста.

## Запуск

```bash
java -jar build/libs/my-blog-back-app-1.0.0-SNAPSHOT.jar
```

или без сборки jar:

```bash
./gradlew bootRun
```

Приложение стартует на http://localhost:8080. Проверка:

```bash
curl "http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5"
```

## Конфигурация

Настройки лежат в `src/main/resources/application.properties`:

```properties
server.port=8080
spring.datasource.url=jdbc:h2:file:~/.my-blog/blog
spring.datasource.username=blog
spring.datasource.password=${DB_PASSWORD:blog}
spring.sql.init.mode=always
```

Данные хранятся в файле `~/.my-blog/blog.mv.db`. Схема (`schema.sql`) накатывается при каждом старте
и не трогает существующие данные (`CREATE TABLE IF NOT EXISTS`).

Пароль к БД передаётся переменной окружения `DB_PASSWORD` (по умолчанию `blog`). Любую другую настройку можно
переопределить без пересборки — переменной окружения или аргументом командной строки:

```bash
DB_PASSWORD=secret java -jar build/libs/my-blog-back-app-1.0.0-SNAPSHOT.jar
java -jar build/libs/my-blog-back-app-1.0.0-SNAPSHOT.jar --server.port=9090
```

## Запуск вместе с фронтендом

Фронтенд обращается к бэкенду по адресу `http://localhost:8080`. Запустите бэкенд, затем фронтенд
(например, `docker compose up` в каталоге фронтенда) и откройте http://localhost.
CORS для `/api/**` разрешён для любых источников.

Особенности фронтенда, учтённые на бэкенде:

- при редактировании комментария фронтенд подставляет в адрес id комментария вместо id поста
  (`PUT /api/posts/{commentId}/comments/{commentId}`), поэтому комментарий ищется только по своему id;
- на странице поста фронтенд запрашивает комментарии до загрузки поста (`/api/posts/undefined/comments`),
  ошибка в ответ сбрасывает уже загруженный пост, поэтому на такой запрос отдаётся пустой список.
