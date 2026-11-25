package com.readscape.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** The author is never part of the request; it comes from the JWT. */
public record CreateReviewRequest(
        @NotNull UUID bookId,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 5000) String body) {}
