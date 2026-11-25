package com.readscape.review;

import com.readscape.common.web.NotFoundException;
import com.readscape.security.CurrentUser;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookCatalog catalog;

    ReviewService(ReviewRepository reviews, BookCatalog catalog) {
        this.reviews = reviews;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public PageView<ReviewView> list(UUID bookId, UUID userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
        return PageView.of(reviews.search(bookId, userId, pageable).map(ReviewView::of));
    }

    @Transactional(readOnly = true)
    public ReviewView get(UUID id) {
        return ReviewView.of(find(id));
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(UUID bookId) {
        long count = reviews.countByBookId(bookId);
        Double avg = reviews.averageRating(bookId);
        return new ReviewSummary(bookId, count, avg == null ? 0.0 : Math.round(avg * 100) / 100.0);
    }

    /** Not transactional on purpose: the remote catalog call must not hold a DB connection. */
    public ReviewView create(CreateReviewRequest request) {
        UUID userId = CurrentUser.userId();
        if (reviews.existsByBookIdAndUserId(request.bookId(), userId)) {
            throw new ConflictException("You have already reviewed this book");
        }
        if (!catalog.exists(request.bookId())) {
            throw new BookNotFoundException("Book does not exist");
        }
        try {
            return ReviewView.of(reviews.saveAndFlush(
                    new Review(request.bookId(), userId, CurrentUser.username(), request.rating(), request.body())));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("You have already reviewed this book");
        }
    }

    @Transactional
    public ReviewView update(UUID id, UpdateReviewRequest request) {
        Review review = find(id);
        if (!review.getUserId().equals(CurrentUser.userId())) {
            throw new AccessDeniedException("Only the author can edit a review");
        }
        review.update(request.rating(), request.body());
        return ReviewView.of(reviews.saveAndFlush(review));
    }

    @Transactional
    public void delete(UUID id) {
        Review review = find(id);
        if (!CurrentUser.isAdmin() && !review.getUserId().equals(CurrentUser.userId())) {
            throw new AccessDeniedException("Only the author or an admin can delete a review");
        }
        reviews.delete(review);
    }

    private Review find(UUID id) {
        return reviews.findById(id).orElseThrow(() -> NotFoundException.of("Review", id));
    }
}
