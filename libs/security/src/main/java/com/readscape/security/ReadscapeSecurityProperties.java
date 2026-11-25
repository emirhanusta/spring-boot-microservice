package com.readscape.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param publicPaths extra unauthenticated paths, each {@code METHOD /pattern} or {@code /pattern} (any method)
 * @param issuer      required {@code iss} claim of accepted tokens
 */
@ConfigurationProperties("readscape.security")
public record ReadscapeSecurityProperties(
        @DefaultValue List<String> publicPaths, @DefaultValue("readscape-identity") String issuer) {}
