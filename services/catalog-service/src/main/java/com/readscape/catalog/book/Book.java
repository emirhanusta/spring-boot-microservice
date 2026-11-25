package com.readscape.catalog.book;

import com.readscape.catalog.author.Author;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
public class Book {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Author author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private int totalCopies = 1;

    @ElementCollection
    @CollectionTable(name = "book_genre", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "genre", nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    private Set<Genre> genres = EnumSet.noneOf(Genre.class);

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Book() {}

    public Book(Author author, String title, String description, LocalDate publishedDate, int totalCopies,
            Set<Genre> genres) {
        update(author, title, description, publishedDate, totalCopies, genres);
    }

    public void update(Author author, String title, String description, LocalDate publishedDate, int totalCopies,
            Set<Genre> genres) {
        this.author = author;
        this.title = title;
        this.description = description;
        this.publishedDate = publishedDate;
        this.totalCopies = totalCopies;
        this.genres.retainAll(genres);
        this.genres.addAll(genres);
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Author getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public int getTotalCopies() { return totalCopies; }
    public Set<Genre> getGenres() { return genres; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
