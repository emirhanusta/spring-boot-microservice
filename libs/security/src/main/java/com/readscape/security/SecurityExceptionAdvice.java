package com.readscape.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps security exceptions thrown from controllers (e.g. {@code @PreAuthorize}) to 401/403 problem details.
 * Runs before common-web's catch-all handler, which would otherwise turn them into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class SecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail denied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
    }
}
