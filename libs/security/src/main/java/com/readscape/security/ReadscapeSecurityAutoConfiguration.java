package com.readscape.security;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Default stateless JWT resource-server setup for servlet services. See libs/security/README.md. */
@AutoConfiguration(
        beforeName = {
            "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
            "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"
        })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@EnableConfigurationProperties(ReadscapeSecurityProperties.class)
@EnableMethodSecurity
public class ReadscapeSecurityAutoConfiguration {

    static final String[] ALWAYS_PUBLIC = {
        "/actuator/health/**", "/actuator/health", "/actuator/info", "/actuator/prometheus",
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain readscapeSecurityFilterChain(HttpSecurity http, ReadscapeSecurityProperties props)
            throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(ReadscapeSecurityAutoConfiguration::rolesToAuthorities);
        converter.setPrincipalClaimName("sub");

        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(ALWAYS_PUBLIC).permitAll();
                    for (String entry : props.publicPaths()) {
                        String[] parts = entry.trim().split("\\s+", 2);
                        if (parts.length == 2) {
                            auth.requestMatchers(HttpMethod.valueOf(parts[0].toUpperCase()), parts[1]).permitAll();
                        } else {
                            auth.requestMatchers(parts[0]).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e.authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDeniedHandler()))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.entryPoint())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.accessDeniedHandler()));
        return http.build();
    }

    /** RS256 decoder against the identity JWKS, with fixed-issuer validation. */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    JwtDecoder jwtDecoder(
            @org.springframework.beans.factory.annotation.Value(
                            "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
                    String jwkSetUri,
            ReadscapeSecurityProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(props.issuer())));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    SecurityExceptionAdvice securityExceptionAdvice() {
        return new SecurityExceptionAdvice();
    }

    private static Collection<GrantedAuthority> rolesToAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }
}
