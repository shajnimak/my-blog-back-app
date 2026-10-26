# my-blog-back-app

Бэкенд веб-приложения «Блог». Отдаёт фронтенду REST API для работы с постами, тегами, лайками,
картинками и комментариями. Приложение собирается в `war` и запускается в сервлет-контейнере Tomcat.

## Стек

- Java 21
- Spring Framework 6.2 (Spring Web MVC, Spring JDBC, Spring TestContext) — **без Spring Boot**
- H2 (файловая БД для приложения, in-memory для тестов)
- Jackson, Logback
- Maven (сборка `war`), Maven Wrapper
- Apache Tomcat 10.1
- JUnit 5, Mockito, AssertJ, MockMvc

## Структура проекта

```
src/main/java/ru/yandex/practicum/blog
├── config       — Java-конфигурация Spring: AppConfig (корневой контекст), WebConfig (MVC, CORS, multipart),
│                  DataSourceConfig (пул соединений, JdbcTemplate, транзакции, накатка schema.sql)
├── controller   — REST-контроллеры и обработчик ошибок
├── service      — бизнес-логика: пагинация, поиск, теги, комментарии
├── dao          — интерфейсы DAO и их реализации на NamedParameterJdbcTemplate
├── model        — доменные модели (Post, Comment, PostSearch)
├── dto          — объекты запросов и ответов REST API
├── mapper       — преобразование моделей в DTO
└── exception    — исключения слоя сервисов
src/main/resources
├── application.properties — параметры подключения к БД
├── schema.sql             — схема БД, накатывается при старте
└── logback.xml
src/main/webapp/WEB-INF/web.xml — поднимает корневой контекст (ContextLoaderListener) и DispatcherServlet
```

Контексты Spring:

- **корневой** (`AppConfig`) — сервисы, DAO, БД;
- **веб** (`WebConfig`) — контроллеры, CORS, загрузка файлов. Дочерний по отношению к корневому.

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

| Метод    | Адрес                                            | Описание                                              |
|----------|--------------------------------------------------|-------------------------------------------------------|
| `GET`    | `/api/posts?search=...&pageNumber=1&pageSize=5`  | Лента постов с поиском и пагинацией                   |
| `GET`    | `/api/posts/{id}`                                | Пост целиком                                          |
| `POST`   | `/api/posts`                                     | Создание поста                                        |
| `PUT`    | `/api/posts/{id}`                                | Редактирование поста                                  |
| `DELETE` | `/api/posts/{id}`                                | Удаление поста вместе с комментариями                 |
| `POST`   | `/api/posts/{id}/likes`                          | +1 лайк, в ответе новое число лайков                  |
| `PUT`    | `/api/posts/{id}/image`                          | Загрузка картинки (`multipart/form-data`, поле `image`) |
| `GET`    | `/api/posts/{id}/image`                          | Картинка поста (байты)                                |
| `GET`    | `/api/posts/{postId}/comments`                   | Комментарии поста                                     |
| `GET`    | `/api/posts/{postId}/comments/{commentId}`       | Комментарий                                           |
| `POST`   | `/api/posts/{postId}/comments`                   | Добавление комментария                                |
| `PUT`    | `/api/posts/{postId}/comments/{commentId}`       | Редактирование комментария                            |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}`       | Удаление комментария                                  |

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
- Apache Tomcat 10.1 (Jakarta Servlet 6.0)
- Maven ставить не обязательно — в проекте есть Maven Wrapper (`./mvnw`)

## Сборка

```bash
./mvnw clean package
```

Тесты запускаются при сборке. Готовый архив — `target/ROOT.war`.

Если в `~/.m2/settings.xml` настроено корпоративное зеркало без доступа к Maven Central, укажите отдельный
файл настроек: `./mvnw -s <путь к settings.xml> clean package`.

## Тесты

```bash
./mvnw test
```

- `dao` — интеграционные тесты DAO на H2 в памяти;
- `controller` — интеграционные MVC-тесты через `MockMvc` на H2 в памяти;
- `service` — тесты сервисов через Spring TestContext, DAO заменены моками `@MockitoBean`;
- `model`, `mapper` — unit-тесты разбора строки поиска и обрезки текста.

Интеграционные тесты наследуют `AbstractIntegrationTest`, тесты сервисов — `AbstractServiceTest`.
Конфигурация внутри каждой группы одинаковая, поэтому TestContext Framework создаёт контексты один раз
и дальше берёт их из кеша. Каждый интеграционный тест выполняется в транзакции, которая откатывается
после теста, — тесты не влияют друг на друга.

## Деплой в Tomcat

1. Установите Tomcat 10.1: скачайте архив с https://tomcat.apache.org/download-10.cgi и распакуйте,
   или на macOS: `brew install tomcat@10`. Каталог Tomcat дальше — `$CATALINA_HOME`
   (для brew это `/opt/homebrew/opt/tomcat@10/libexec`).
2. Соберите `war` (см. «Сборка»).
3. Удалите стандартное приложение `ROOT` и скопируйте `ROOT.war` в `webapps`, чтобы API было доступно
   по корню `http://localhost:8080/api/...`:
   ```bash
   rm -rf $CATALINA_HOME/webapps/ROOT
   cp target/ROOT.war $CATALINA_HOME/webapps/
   ```
4. Запустите Tomcat под JDK 21:
   ```bash
   JAVA_HOME=<путь к JDK 21> $CATALINA_HOME/bin/catalina.sh run
   ```
   В логе должно появиться `Root WebApplicationContext initialized` и `Server startup in ...`.
5. Проверьте:
   ```bash
   curl "http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5"
   ```

Для повторного деплоя достаточно остановить Tomcat (`catalina.sh stop`), удалить из `webapps` каталог
`ROOT` и `ROOT.war`, скопировать новый архив и снова запустить.

## Конфигурация БД

Параметры подключения лежат в `src/main/resources/application.properties`:

```properties
db.url=jdbc:h2:file:~/.my-blog/blog
db.username=blog
db.password=blog
```

Данные хранятся в файле `~/.my-blog/blog.mv.db`. Схема (`schema.sql`) накатывается при каждом старте
и не трогает существующие данные (`CREATE TABLE IF NOT EXISTS`).

Любой параметр можно переопределить без пересборки — переменной окружения или системным свойством JVM:

```bash
export DB_PASSWORD=secret
# или
export CATALINA_OPTS="-Ddb.password=secret"
```

## Запуск вместе с фронтендом

Фронтенд обращается к бэкенду по адресу `http://localhost:8080`. Запустите бэкенд в Tomcat, затем
фронтенд (например, `docker compose up` в каталоге фронтенда) и откройте http://localhost.
CORS для `/api/**` разрешён для любых источников.

Особенности фронтенда, учтённые на бэкенде:

- при редактировании комментария фронтенд подставляет в адрес id комментария вместо id поста
  (`PUT /api/posts/{commentId}/comments/{commentId}`), поэтому комментарий ищется только по своему id;
- на странице поста фронтенд запрашивает комментарии до загрузки поста (`/api/posts/undefined/comments`),
  ошибка в ответ сбрасывает уже загруженный пост, поэтому на такой запрос отдаётся пустой список.
