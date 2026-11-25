package com.readscape.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review")
public class Review {

    @Id
    private UUID id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private UUID bookId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private short rating;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Review() {}

    public Review(UUID bookId, UUID userId, String username, int rating, String body) {
        this.id = UUID.randomUUID();
        this.bookId = bookId;
        this.userId = userId;
        this.username = username;
        this.rating = (short) rating;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(int rating, String body) {
        this.rating = (short) rating;
        this.body = body;
    }

    public UUID getId() { return id; }
    public UUID getBookId() { return bookId; }
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
