package com.readscape.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@Validated
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public PageView<ReviewView> list(
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviews.list(bookId, userId, page, size);
    }

    @GetMapping("/{id}")
    public ReviewView get(@PathVariable UUID id) {
        return reviews.get(id);
    }

    @GetMapping("/books/{bookId}/summary")
    public ReviewSummary summary(@PathVariable UUID bookId) {
        return reviews.summary(bookId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView create(@Valid @RequestBody CreateReviewRequest request) {
        return reviews.create(request);
    }

    @PutMapping("/{id}")
    public ReviewView update(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest request) {
        return reviews.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        reviews.delete(id);
    }
}
