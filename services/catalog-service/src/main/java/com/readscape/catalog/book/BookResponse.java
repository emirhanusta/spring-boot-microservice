package com.readscape.catalog.book;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String title,
        String description,
        LocalDate publishedDate,
        int totalCopies,
        List<Genre> genres,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static BookResponse of(Book b) {
        return new BookResponse(b.getId(), b.getAuthor().getId(), b.getAuthor().getName(), b.getTitle(),
                b.getDescription(), b.getPublishedDate(), b.getTotalCopies(),
                b.getGenres().stream().sorted().toList(), b.getCreatedAt(), b.getUpdatedAt(), b.getVersion());
    }
}
