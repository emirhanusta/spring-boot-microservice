package com.readscape.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewView(
        UUID id, UUID bookId, UUID userId, String username, int rating, String body,
        Instant createdAt, Instant updatedAt) {

    static ReviewView of(Review r) {
        return new ReviewView(r.getId(), r.getBookId(), r.getUserId(), r.getUsername(), r.getRating(),
                r.getBody(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
