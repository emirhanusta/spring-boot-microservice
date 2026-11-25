package com.readscape.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.readscape.testfixtures.TestJwts;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;

/** Gateway behaviour against three in-test upstreams (JDK HttpServer) that record what they receive. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GatewayTest.TestDecoder.class)
class GatewayTest {

    record Received(String method, String path, Map<String, List<String>> headers) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .findFirst()
                    .map(e -> e.getValue().get(0))
                    .orElse(null);
        }
    }

    /** Upstream that answers 200 with its own name as body and remembers every request. */
    static final class Upstream {
        final String name;
        final HttpServer server;
        final List<Received> received = new CopyOnWriteArrayList<>();

        Upstream(String name) {
            this.name = name;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            server.createContext("/", ex -> {
                received.add(new Received(
                        ex.getRequestMethod(), ex.getRequestURI().getPath(), Map.copyOf(ex.getRequestHeaders())));
                byte[] body = name.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }
    }

    static final Upstream IDENTITY = new Upstream("identity");
    static final Upstream CATALOG = new Upstream("catalog");
    static final Upstream REVIEW = new Upstream("review");

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDecoder {
        @Bean
        @Primary
        ReactiveJwtDecoder testReactiveJwtDecoder() {
            var decoder = TestJwts.decoder();
            return token -> Mono.fromCallable(() -> decoder.decode(token));
        }
    }

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("IDENTITY_URL", IDENTITY::url);
        registry.add("CATALOG_URL", CATALOG::url);
        registry.add("REVIEW_URL", REVIEW::url);
    }

    @AfterAll
    static void stop() {
        IDENTITY.server.stop(0);
        CATALOG.server.stop(0);
        REVIEW.server.stop(0);
    }

    @LocalServerPort
    int port;

    @Autowired
    ReactiveJwtDecoder decoder;

    final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void reset() {
        IDENTITY.received.clear();
        CATALOG.received.clear();
        REVIEW.received.clear();
    }

    private HttpResponse<String> send(String method, String path, String... headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (headers.length > 0) {
            b.headers(headers);
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> r = send("GET", "/actuator/health");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void routesEachPrefixToItsService() throws Exception {
        Map<String, Upstream> expectations = Map.of(
                "/api/v1/auth/login", IDENTITY,
                "/api/v1/users/me", IDENTITY,
                "/.well-known/jwks.json", IDENTITY,
                "/api/v1/authors/1", CATALOG,
                "/api/v1/books", CATALOG,
                "/api/v1/reviews/abc", REVIEW);
        for (var e : expectations.entrySet()) {
            HttpResponse<String> r = send("GET", e.getKey());
            assertThat(r.statusCode()).as(e.getKey()).isEqualTo(200);
            assertThat(r.body()).as(e.getKey()).isEqualTo(e.getValue().name);
            assertThat(e.getValue().received).as(e.getKey()).extracting(Received::path).contains(e.getKey());
        }
    }

    @Test
    void unknownPathIsNotRouted() throws Exception {
        assertThat(send("GET", "/api/v1/unknown").statusCode()).isEqualTo(404);
    }

    @Test
    void apiDocsAreRewrittenToEachService() throws Exception {
        assertThat(send("GET", "/v3/api-docs/identity").body()).isEqualTo("identity");
        assertThat(send("GET", "/v3/api-docs/catalog").body()).isEqualTo("catalog");
        assertThat(send("GET", "/v3/api-docs/review").body()).isEqualTo("review");
        assertThat(IDENTITY.received).extracting(Received::path).containsExactly("/v3/api-docs");
        assertThat(CATALOG.received).extracting(Received::path).containsExactly("/v3/api-docs");
        assertThat(REVIEW.received).extracting(Received::path).containsExactly("/v3/api-docs");
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        HttpResponse<String> r = send("GET", "/swagger-ui/index.html");
        assertThat(r.statusCode()).isEqualTo(200);
    }

    @Test
    void requestWithoutTokenReachesUpstream() throws Exception {
        HttpResponse<String> r = send("GET", "/api/v1/books");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(CATALOG.received).hasSize(1);
        assertThat(CATALOG.received.get(0).header("Authorization")).isNull();
    }

    @Test
    void validTokenIsForwardedUntouched() throws Exception {
        String bearer = TestJwts.bearer(UUID.randomUUID(), "alice", "USER");
        HttpResponse<String> r = send("POST", "/api/v1/reviews", "Authorization", bearer);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(REVIEW.received).hasSize(1);
        assertThat(REVIEW.received.get(0).header("Authorization")).isEqualTo(bearer);
    }

    @Test
    void invalidTokensAreRejectedAtTheGateway() throws Exception {
        UUID id = UUID.randomUUID();
        String expired = "Bearer " + TestJwts.token(id, "alice", TestJwts.ISSUER, Duration.ofMinutes(-5), List.of("USER"));
        String wrongIssuer =
                "Bearer " + TestJwts.token(id, "alice", "evil-issuer", Duration.ofMinutes(5), List.of("ADMIN"));
        String tampered = TestJwts.bearer(id, "alice", "USER");
        tampered = tampered.substring(0, tampered.length() - 4) + "AAAA";
        for (String header : List.of(expired, wrongIssuer, tampered, "Bearer not-a-jwt")) {
            HttpResponse<String> r = send("GET", "/api/v1/books", "Authorization", header);
            assertThat(r.statusCode()).as(header).isEqualTo(401);
            assertThat(r.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
            assertThat(r.body()).contains("\"status\":401");
        }
        assertThat(CATALOG.received).isEmpty();
    }

    @Test
    void userHeadersFromClientsAreStripped() throws Exception {
        send("GET", "/api/v1/books", "X-User-Id", "attacker", "x-user-roles", "ADMIN", "X-Other", "kept");
        assertThat(CATALOG.received).hasSize(1);
        Received r = CATALOG.received.get(0);
        assertThat(r.header("X-User-Id")).isNull();
        assertThat(r.header("X-User-Roles")).isNull();
        assertThat(r.header("X-Other")).isEqualTo("kept");
    }
}
