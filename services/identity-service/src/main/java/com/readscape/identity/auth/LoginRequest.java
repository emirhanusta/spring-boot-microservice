package com.readscape.identity.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 254) String username, @NotBlank @Size(max = 72) String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}
