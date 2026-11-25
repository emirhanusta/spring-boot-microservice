# libs/security

Shared servlet OAuth2 resource-server setup. Add `implementation(project(":libs:security"))` and (for tests)
`testImplementation(project(":libs:test-fixtures"))`. The auto-configuration
(`ReadscapeSecurityAutoConfiguration`) is picked up automatically in servlet apps.

## Default `SecurityFilterChain`
Stateless, CSRF off, JWT resource server, method security enabled (`@PreAuthorize` works).
- Always public: `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, `/v3/api-docs/**`,
  `/swagger-ui/**`, `/swagger-ui.html`.
- Extra public paths: `readscape.security.public-paths` (list; each `METHOD /pattern` or `/pattern`):
  ```yaml
  readscape.security.public-paths:
    - GET /api/v1/books/**
    - GET /api/v1/authors/**
  ```
- Everything else requires a valid token. 401 / 403 are RFC 9457 `application/problem+json`.
  `AccessDeniedException` thrown from controllers (`@PreAuthorize`) is also mapped to 403 by an advice
  in this lib (it is ordered ahead of common-web's catch-all, which would otherwise yield 500).
- Backs off if the service defines its own `SecurityFilterChain`.

## JWT
- Decoder: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` (set it to
  `${IDENTITY_JWKS_URI:http://localhost:8001/.well-known/jwks.json}`), RS256 only, issuer must equal
  `readscape.security.issuer` (default `readscape-identity`), no audience check. A service that defines its
  own `JwtDecoder` bean (identity-service does) replaces it.
- Claims: `sub` = user UUID (principal name), `preferred_username`, `roles` = array like `["USER"]` or
  `["ADMIN"]` -> authorities `ROLE_USER` / `ROLE_ADMIN` (`hasRole('ADMIN')`).
- `com.readscape.security.CurrentUser` (static): `userId()` (UUID), `username()`, `isAdmin()`.

## Test support (in `libs/test-fixtures`, package `com.readscape.testfixtures`)
- `TestJwts.token(UUID id, String username, String... roles)` / `TestJwts.bearer(...)` (returns
  `"Bearer <jwt>"`); full form `token(id, username, issuer, ttl, roles)` (negative ttl = expired token).
- `TestJwtDecoderConfig` - `@Import` it in a `@SpringBootTest` so the service trusts `TestJwts` tokens
  (a `@Primary` `JwtDecoder`; no JWKS endpoint needed). `TestJwts.decoder()` gives the raw decoder.

```java
@SpringBootTest @AutoConfigureMockMvc
@Import({PostgresContainerConfig.class, TestJwtDecoderConfig.class})
class XTest {
  @Autowired MockMvc mvc;
  @Test void t() throws Exception {
    mvc.perform(get("/api/v1/x").header("Authorization", TestJwts.bearer(UUID.randomUUID(), "alice", "USER")))
       .andExpect(status().isOk());
  }
}
```
(`AutoConfigureMockMvc` is `org.springframework.boot.webmvc.test.autoconfigure` in Boot 4; add
`spring-boot-starter-webmvc-test` as a test dependency.)
