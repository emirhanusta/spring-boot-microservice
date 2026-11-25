package com.readscape.catalog.author;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AuthorResponse(
        UUID id, String name, String biography, LocalDate birthDate, Instant createdAt, Instant updatedAt, long version) {

    static AuthorResponse of(Author a) {
        return new AuthorResponse(a.getId(), a.getName(), a.getBiography(), a.getBirthDate(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
