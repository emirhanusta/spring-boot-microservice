package com.readscape.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Static accessor for the authenticated caller (JWT {@code sub}, {@code preferred_username}, {@code roles}). */
public final class CurrentUser {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private CurrentUser() {}

    public static UUID userId() {
        return UUID.fromString(authentication().getName());
    }

    public static String username() {
        if (authentication().getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("preferred_username");
            if (name != null) {
                return name;
            }
        }
        return authentication().getName();
    }

    public static boolean isAdmin() {
        return authentication().getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    private static Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return auth;
    }
}
