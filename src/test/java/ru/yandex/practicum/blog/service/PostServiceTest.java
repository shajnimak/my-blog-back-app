package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.blog.dto.NewPostDto;
import ru.yandex.practicum.blog.dto.PostDto;
import ru.yandex.practicum.blog.dto.PostPageDto;
import ru.yandex.practicum.blog.dto.UpdatePostDto;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.exception.ValidationException;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostServiceTest extends AbstractServiceTest {

    @Autowired
    private PostService postService;

    @Test
    void findPostsReturnsPageWithPreviewsAndTags() {
        String longText = "a".repeat(200);
        PostSearch criteria = new PostSearch("spring", Set.of("java"));
        when(postDao.count(criteria)).thenReturn(12);
        when(postDao.findAll(criteria, 5, 5)).thenReturn(List.of(
                new Post(7L, "Spring", longText, List.of(), 3, 1),
                new Post(6L, "Spring MVC", "короткий текст", List.of(), 0, 0)
        ));
        when(tagDao.findByPostIds(List.of(7L, 6L))).thenReturn(Map.of(7L, List.of("java", "spring")));

        PostPageDto page = postService.findPosts("spring #java", 2, 5);

        assertThat(page.hasPrev()).isTrue();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.lastPage()).isEqualTo(3);
        assertThat(page.posts()).containsExactly(
                new PostDto(7L, "Spring", "a".repeat(128) + "…", List.of("java", "spring"), 3, 1),
                new PostDto(6L, "Spring MVC", "короткий текст", List.of(), 0, 0)
        );
    }

    @Test
    void findPostsOnLastPageHasNoNext() {
        when(postDao.count(any())).thenReturn(10);
        when(postDao.findAll(any(), anyInt(), anyInt())).thenReturn(List.of());

        PostPageDto page = postService.findPosts("", 2, 5);

        assertThat(page.hasPrev()).isTrue();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.lastPage()).isEqualTo(2);
    }

    @Test
    void findPostsWithoutResultsReturnsSingleEmptyPage() {
        when(postDao.count(any())).thenReturn(0);

        PostPageDto page = postService.findPosts("нет такого", 1, 5);

        assertThat(page).isEqualTo(new PostPageDto(List.of(), false, false, 1));
        verify(postDao, never()).findAll(any(), anyInt(), anyInt());
    }

    @Test
    void findPostsRejectsInvalidPagination() {
        assertThatThrownBy(() -> postService.findPosts("", 0, 5)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> postService.findPosts("", 1, 0)).isInstanceOf(ValidationException.class);
        verifyNoInteractions(postDao);
    }

    @Test
    void getPostReturnsFullTextWithTags() {
        String longText = "b".repeat(300);
        when(postDao.findById(1L)).thenReturn(Optional.of(new Post(1L, "Пост", longText, List.of(), 5, 2)));
        when(tagDao.findByPostId(1L)).thenReturn(List.of("tag_1"));

        assertThat(postService.getPost(1L)).isEqualTo(new PostDto(1L, "Пост", longText, List.of("tag_1"), 5, 2));
    }

    @Test
    void getPostThrowsWhenNotFound() {
        when(postDao.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createPostSavesPostWithNormalizedTags() {
        when(postDao.create("Пост", "Текст")).thenReturn(3L);
        when(postDao.findById(3L)).thenReturn(Optional.of(new Post(3L, "Пост", "Текст", List.of(), 0, 0)));
        when(tagDao.findByPostId(3L)).thenReturn(List.of("java", "spring"));

        PostDto created = postService.createPost(
                new NewPostDto("Пост", "Текст", Arrays.asList(" java ", "spring", "java", "", null)));

        assertThat(created).isEqualTo(new PostDto(3L, "Пост", "Текст", List.of("java", "spring"), 0, 0));
        verify(tagDao).replacePostTags(3L, List.of("java", "spring"));
    }

    @Test
    void createPostRejectsBlankTitle() {
        assertThatThrownBy(() -> postService.createPost(new NewPostDto(" ", "Текст", List.of())))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(postDao, tagDao);
    }

    @Test
    void updatePostChangesPostAndTags() {
        when(postDao.update(3L, "Новое", "Новый текст")).thenReturn(true);
        when(postDao.findById(3L)).thenReturn(Optional.of(new Post(3L, "Новое", "Новый текст", List.of(), 4, 1)));
        when(tagDao.findByPostId(3L)).thenReturn(List.of("kotlin"));

        PostDto updated = postService.updatePost(3L, new UpdatePostDto(3L, "Новое", "Новый текст", List.of("kotlin")));

        assertThat(updated).isEqualTo(new PostDto(3L, "Новое", "Новый текст", List.of("kotlin"), 4, 1));
        verify(tagDao).replacePostTags(3L, List.of("kotlin"));
    }

    @Test
    void updatePostThrowsWhenNotFound() {
        when(postDao.update(anyLong(), anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> postService.updatePost(3L, new UpdatePostDto(3L, "Пост", "Текст", List.of())))
                .isInstanceOf(NotFoundException.class);
        verify(tagDao, never()).replacePostTags(anyLong(), any());
    }

    @Test
    void updatePostRejectsMismatchedId() {
        assertThatThrownBy(() -> postService.updatePost(3L, new UpdatePostDto(4L, "Пост", "Текст", List.of())))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(postDao);
    }

    @Test
    void deletePost() {
        when(postDao.deleteById(1L)).thenReturn(true);

        postService.deletePost(1L);

        verify(postDao).deleteById(1L);
    }

    @Test
    void deletePostThrowsWhenNotFound() {
        when(postDao.deleteById(1L)).thenReturn(false);

        assertThatThrownBy(() -> postService.deletePost(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void addLikeReturnsNewLikesCount() {
        when(postDao.incrementLikes(1L)).thenReturn(Optional.of(6));

        assertThat(postService.addLike(1L)).isEqualTo(6);
    }

    @Test
    void addLikeThrowsWhenNotFound() {
        when(postDao.incrementLikes(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.addLike(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateImage() {
        byte[] image = {1, 2, 3};
        when(postDao.updateImage(1L, image)).thenReturn(true);

        postService.updateImage(1L, image);

        verify(postDao).updateImage(1L, image);
    }

    @Test
    void updateImageRejectsEmptyFile() {
        assertThatThrownBy(() -> postService.updateImage(1L, new byte[0])).isInstanceOf(ValidationException.class);
        verifyNoInteractions(postDao);
    }

    @Test
    void updateImageThrowsWhenPostNotFound() {
        when(postDao.updateImage(anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> postService.updateImage(1L, new byte[]{1})).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getImage() {
        when(postDao.findImage(1L)).thenReturn(Optional.of(new byte[]{1, 2}));

        assertThat(postService.getImage(1L)).containsExactly(1, 2);
    }

    @Test
    void getImageThrowsWhenAbsent() {
        when(postDao.findImage(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getImage(1L)).isInstanceOf(NotFoundException.class);
    }
}
