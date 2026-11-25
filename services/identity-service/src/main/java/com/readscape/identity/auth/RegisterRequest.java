package com.readscape.identity.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Unknown JSON properties (notably {@code role}) are ignored, so a role can never be requested. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, '.', '_' and '-'") String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=****]";
    }
}
