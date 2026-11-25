package com.readscape.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 401 / 403 writers that emit RFC 9457 {@code application/problem+json} (and keep the Bearer challenge header). */
final class ProblemDetailSecurityHandlers {

    private ProblemDetailSecurityHandlers() {}

    static AuthenticationEntryPoint entryPoint() {
        BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) -> {
            delegate.commence(req, res, ex);
            write(res, HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        };
    }

    static AccessDeniedHandler accessDeniedHandler() {
        BearerTokenAccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();
        return (HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) -> {
            delegate.handle(req, res, ex);
            write(res, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
        };
    }

    private static void write(HttpServletResponse res, HttpStatus status, String detail) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}"
                .formatted(status.getReasonPhrase(), status.value(), detail));
    }
}
