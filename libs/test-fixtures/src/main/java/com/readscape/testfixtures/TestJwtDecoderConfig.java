package com.readscape.testfixtures;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** {@code @Import} into a test so the service accepts tokens minted by {@link TestJwts}. */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtDecoderConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return TestJwts.decoder();
    }
}
