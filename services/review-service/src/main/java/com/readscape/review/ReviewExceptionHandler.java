package com.readscape.review;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Runs before common-web's catch-all so these map to 409 / 422 / 503 instead of 500. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ReviewExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Review conflicts with existing data");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail stale(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Review was modified concurrently; retry");
    }

    @ExceptionHandler(BookNotFoundException.class)
    ProblemDetail noBook(BookNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail catalogDown(CatalogUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
