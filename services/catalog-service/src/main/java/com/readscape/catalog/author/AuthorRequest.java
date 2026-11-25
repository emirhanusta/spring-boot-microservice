package com.readscape.catalog.author;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** {@code version} is optional; when sent on PUT and stale, the update is rejected with 409. */
public record AuthorRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 10000) String biography,
        @Past LocalDate birthDate,
        Long version) {}
