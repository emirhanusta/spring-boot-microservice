package com.readscape.catalog.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** {@code totalCopies} defaults to 1 and {@code genres} to empty; {@code version} is optional (409 when stale). */
public record BookRequest(
        @NotNull UUID authorId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 10000) String description,
        LocalDate publishedDate,
        @Min(0) Integer totalCopies,
        Set<@NotNull Genre> genres,
        Long version) {}
