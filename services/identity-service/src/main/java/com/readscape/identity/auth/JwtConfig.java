package com.readscape.identity.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Signing key, JWT encoder and the service's own (local, no HTTP) decoder. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    public static final String ISSUER = "readscape-identity";

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    RSAKey signingKey(JwtProperties props, ResourceLoader loader) throws Exception {
        RSAPublicKey pub;
        RSAPrivateKey priv;
        if (props.hasKeys()) {
            priv = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(pem(loader, props.privateKeyLocation())));
            pub = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(pem(loader, props.publicKeyLocation())));
        } else {
            log.warn("No readscape.jwt.*-key-location configured: generating an EPHEMERAL RSA key pair. "
                    + "Tokens become invalid on restart; do not use this outside local development.");
            RSAKey generated = new RSAKeyGenerator(2048).generate();
            pub = generated.toRSAPublicKey();
            priv = generated.toRSAPrivateKey();
        }
        RSAKey unnamed = new RSAKey.Builder(pub).privateKey(priv).keyUse(KeyUse.SIGNATURE)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).build();
        // kid = RFC 7638 thumbprint, so it is stable for a given key.
        return new RSAKey.Builder(unnamed).keyID(unnamed.computeThumbprint().toString()).build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey signingKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
    }

    /** Validates incoming tokens against the local public key; its presence makes libs/security back off. */
    @Bean
    JwtDecoder jwtDecoder(RSAKey signingKey) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(ISSUER)));
        return decoder;
    }

    private static byte[] pem(ResourceLoader loader, String location) {
        try (var in = loader.getResource(location).getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.US_ASCII)
                    .replaceAll("-----(BEGIN|END)[A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(text);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read key at " + location, e);
        }
    }
}
