package com.readscape.identity.user;

import java.time.Instant;
import java.util.UUID;

/** The only representation of a user that leaves the service: deliberately has no credential fields. */
public record UserView(UUID id, String username, String email, Role role, Instant createdAt, Instant updatedAt) {

    public static UserView from(User u) {
        return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
