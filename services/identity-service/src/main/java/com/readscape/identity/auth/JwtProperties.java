package com.readscape.identity.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Blank key locations mean "generate an ephemeral key pair". */
@ConfigurationProperties("readscape.jwt")
public record JwtProperties(String privateKeyLocation, String publicKeyLocation, @DefaultValue("15m") Duration ttl) {

    boolean hasKeys() {
        return privateKeyLocation != null && !privateKeyLocation.isBlank()
                && publicKeyLocation != null && !publicKeyLocation.isBlank();
    }
}
