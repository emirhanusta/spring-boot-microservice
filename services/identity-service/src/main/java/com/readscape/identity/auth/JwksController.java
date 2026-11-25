package com.readscape.identity.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey signingKey) {
        this.jwks = new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwks;
    }
}
