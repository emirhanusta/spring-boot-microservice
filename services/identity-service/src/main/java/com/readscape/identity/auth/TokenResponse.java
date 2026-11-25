package com.readscape.identity.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
