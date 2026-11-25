package com.readscape.testfixtures;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Signs RS256 test tokens with a per-JVM RSA key; {@link #decoder()} trusts exactly that key. */
public final class TestJwts {

    public static final String ISSUER = "readscape-identity";
    public static final String KEY_ID = "test-key";

    private static final RSAKey KEY = generate();
    private static final NimbusJwtEncoder ENCODER =
            new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));

    private TestJwts() {}

    /** Valid 15-minute token issued by {@value #ISSUER}. */
    public static String token(UUID userId, String username, String... roles) {
        return token(userId, username, ISSUER, Duration.ofMinutes(15), List.of(roles));
    }

    public static String token(UUID userId, String username, String issuer, Duration ttl, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        Instant issuedAt = expiresAt.isAfter(now) ? now : expiresAt.minusSeconds(60); // negative ttl = expired token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("preferred_username", username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build();
        return ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** {@code Authorization} header value for {@link #token(UUID, String, String...)}. */
    public static String bearer(UUID userId, String username, String... roles) {
        return "Bearer " + token(userId, username, roles);
    }

    /** Decoder that trusts the test key and enforces issuer {@value #ISSUER}. */
    public static JwtDecoder decoder() {
        try {
            NimbusJwtDecoder decoder =
                    NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(ISSUER)));
            return decoder;
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey generate() {
        try {
            return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
