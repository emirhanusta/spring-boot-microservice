package com.readscape.identity.config;

import com.readscape.identity.auth.InvalidCredentialsException;
import com.readscape.identity.user.ConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Runs before common-web's catch-all so these map to 409 / 401 instead of 500. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentityExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Lost a race on the unique constraints. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username or email is already in use");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail badCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
