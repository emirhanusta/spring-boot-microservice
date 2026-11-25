package com.readscape.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestBookCatalogTest {

    HttpServer server;
    RestBookCatalog catalog;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/books/", ex -> {
            String id = ex.getRequestURI().getPath().substring("/api/v1/books/".length());
            int code = switch (id.charAt(0)) { case '1' -> 200; case '2' -> 404; default -> 500; };
            ex.sendResponseHeaders(code, -1);
            ex.close();
        });
        server.start();
        catalog = new RestBookCatalog(CatalogConfig.catalogApi("http://127.0.0.1:" + server.getAddress().getPort()));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void mapsStatuses() {
        assertThat(catalog.exists(UUID.fromString("10000000-0000-0000-0000-000000000000"))).isTrue();
        assertThat(catalog.exists(UUID.fromString("20000000-0000-0000-0000-000000000000"))).isFalse();
        assertThatThrownBy(() -> catalog.exists(UUID.fromString("30000000-0000-0000-0000-000000000000")))
                .isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void unreachableIsUnavailable() {
        server.stop(0);
        assertThatThrownBy(() -> catalog.exists(UUID.randomUUID())).isInstanceOf(CatalogUnavailableException.class);
    }
}
