package com.readscape.catalog.author;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Author {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String biography;

    private LocalDate birthDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Author() {}

    public Author(String name, String biography, LocalDate birthDate) {
        update(name, biography, birthDate);
    }

    public void update(String name, String biography, LocalDate birthDate) {
        this.name = name;
        this.biography = biography;
        this.birthDate = birthDate;
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
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public LocalDate getBirthDate() { return birthDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
