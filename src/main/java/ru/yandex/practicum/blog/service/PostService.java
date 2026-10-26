package ru.yandex.practicum.blog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.dao.PostDao;
import ru.yandex.practicum.blog.dao.TagDao;
import ru.yandex.practicum.blog.dto.NewPostDto;
import ru.yandex.practicum.blog.dto.PostDto;
import ru.yandex.practicum.blog.dto.PostPageDto;
import ru.yandex.practicum.blog.dto.UpdatePostDto;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.exception.ValidationException;
import ru.yandex.practicum.blog.mapper.PostMapper;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearch;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostDao postDao;
    private final TagDao tagDao;

    public PostService(PostDao postDao, TagDao tagDao) {
        this.postDao = postDao;
        this.tagDao = tagDao;
    }

    @Transactional(readOnly = true)
    public PostPageDto findPosts(String search, int pageNumber, int pageSize) {
        if (pageNumber < 1 || pageSize < 1) {
            throw new ValidationException("Номер страницы и размер страницы должны быть больше нуля");
        }

        PostSearch criteria = PostSearch.parse(search);
        int total = postDao.count(criteria);
        int lastPage = Math.max(1, (int) ((total + (long) pageSize - 1) / pageSize));
        if (total == 0 || pageNumber > lastPage) {
            return new PostPageDto(List.of(), pageNumber > 1, false, lastPage);
        }

        List<Post> posts = postDao.findAll(criteria, pageSize, (pageNumber - 1) * pageSize);
        Map<Long, List<String>> tagsByPostId = tagDao.findByPostIds(posts.stream().map(Post::id).toList());
        List<PostDto> dtos = posts.stream()
                .map(post -> post.withTags(tagsByPostId.getOrDefault(post.id(), List.of())))
                .map(PostMapper::toPreviewDto)
                .toList();

        return new PostPageDto(dtos, pageNumber > 1, pageNumber < lastPage, lastPage);
    }

    @Transactional(readOnly = true)
    public PostDto getPost(long id) {
        return PostMapper.toDto(loadPost(id));
    }

    @Transactional
    public PostDto createPost(NewPostDto dto) {
        validate(dto.title(), dto.text());
        long id = postDao.create(dto.title(), dto.text());
        tagDao.replacePostTags(id, normalizeTags(dto.tags()));
        return PostMapper.toDto(loadPost(id));
    }

    @Transactional
    public PostDto updatePost(long id, UpdatePostDto dto) {
        validate(dto.title(), dto.text());
        if (dto.id() != null && dto.id() != id) {
            throw new ValidationException("id поста в теле запроса не совпадает с id в адресе");
        }
        if (!postDao.update(id, dto.title(), dto.text())) {
            throw NotFoundException.post(id);
        }
        tagDao.replacePostTags(id, normalizeTags(dto.tags()));
        return PostMapper.toDto(loadPost(id));
    }

    @Transactional
    public void deletePost(long id) {
        if (!postDao.deleteById(id)) {
            throw NotFoundException.post(id);
        }
    }

    @Transactional
    public int addLike(long id) {
        return postDao.incrementLikes(id).orElseThrow(() -> NotFoundException.post(id));
    }

    @Transactional
    public void updateImage(long id, byte[] image) {
        if (image == null || image.length == 0) {
            throw new ValidationException("Файл картинки пустой");
        }
        if (!postDao.updateImage(id, image)) {
            throw NotFoundException.post(id);
        }
    }

    @Transactional(readOnly = true)
    public byte[] getImage(long id) {
        return postDao.findImage(id)
                .orElseThrow(() -> new NotFoundException("Картинка поста с id=" + id + " не найдена"));
    }

    private Post loadPost(long id) {
        Post post = postDao.findById(id).orElseThrow(() -> NotFoundException.post(id));
        return post.withTags(tagDao.findByPostId(id));
    }

    private static void validate(String title, String text) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Название поста не может быть пустым");
        }
        if (text == null) {
            throw new ValidationException("Текст поста обязателен");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return List.copyOf(tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
