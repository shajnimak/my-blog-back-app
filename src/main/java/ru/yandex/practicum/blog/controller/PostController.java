package ru.yandex.practicum.blog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.blog.dto.NewPostDto;
import ru.yandex.practicum.blog.dto.PostDto;
import ru.yandex.practicum.blog.dto.PostPageDto;
import ru.yandex.practicum.blog.dto.UpdatePostDto;
import ru.yandex.practicum.blog.service.PostService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLConnection;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PostPageDto getPosts(@RequestParam String search,
                                @RequestParam int pageNumber,
                                @RequestParam int pageSize) {
        return postService.findPosts(search, pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public PostDto getPost(@PathVariable long id) {
        return postService.getPost(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto createPost(@RequestBody NewPostDto post) {
        return postService.createPost(post);
    }

    @PutMapping("/{id}")
    public PostDto updatePost(@PathVariable long id, @RequestBody UpdatePostDto post) {
        return postService.updatePost(id, post);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable long id) {
        postService.deletePost(id);
    }

    @PostMapping("/{id}/likes")
    public int addLike(@PathVariable long id) {
        return postService.addLike(id);
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void updateImage(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        try {
            postService.updateImage(id, image.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать загруженную картинку", e);
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable long id) {
        byte[] image = postService.getImage(id);
        return ResponseEntity.ok()
                .contentType(detectContentType(image))
                .body(image);
    }

    private static MediaType detectContentType(byte[] image) {
        try {
            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(image));
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
