package com.readscape.catalog.author;

import com.readscape.catalog.book.BookResponse;
import com.readscape.catalog.book.BookService;
import com.readscape.catalog.common.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authors;
    private final BookService books;

    public AuthorController(AuthorService authors, BookService books) {
        this.authors = authors;
        this.books = books;
    }

    @GetMapping
    public PageView<AuthorResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return authors.list(page, size);
    }

    @GetMapping("/{id}")
    public AuthorResponse get(@PathVariable UUID id) {
        return authors.get(id);
    }

    @GetMapping("/{id}/books")
    public PageView<BookResponse> books(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return books.listByAuthor(id, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorResponse create(@Valid @RequestBody AuthorRequest request) {
        return authors.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorResponse update(@PathVariable UUID id, @Valid @RequestBody AuthorRequest request) {
        return authors.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        authors.delete(id);
    }
}
