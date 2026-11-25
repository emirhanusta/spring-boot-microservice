package com.readscape.catalog.book;

import com.readscape.catalog.storage.CoverStorage;
import com.readscape.common.web.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/books/{id}/cover")
public class CoverController {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final BookService books;
    private final CoverStorage storage;

    public CoverController(BookService books, CoverStorage storage) {
        this.books = books;
        this.storage = storage;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        books.find(id);
        String type = file.getContentType();
        if (type == null || !TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Cover must be image/png, image/jpeg or image/webp");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cover file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Cover exceeds the 5 MB limit");
        }
        try {
            storage.put(CoverStorage.keyFor(id), file.getBytes(), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping
    public ResponseEntity<InputStreamResource> get(@PathVariable UUID id) {
        books.find(id);
        var cover = storage.get(CoverStorage.keyFor(id))
                .orElseThrow(() -> new NotFoundException("Book '%s' has no cover".formatted(id)));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .contentLength(cover.contentLength())
                .body(new InputStreamResource(cover.content()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        books.find(id);
        storage.delete(CoverStorage.keyFor(id));
    }
}
