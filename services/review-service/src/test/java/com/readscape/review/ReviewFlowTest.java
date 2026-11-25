package com.readscape.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.readscape.testfixtures.PostgresContainerConfig;
import com.readscape.testfixtures.TestJwtDecoderConfig;
import com.readscape.testfixtures.TestJwts;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresContainerConfig.class, TestJwtDecoderConfig.class, ReviewFlowTest.StubCatalog.class})
class ReviewFlowTest {

    static final UUID MISSING_BOOK = UUID.fromString("00000000-0000-0000-0000-000000000404");
    static final AtomicBoolean CATALOG_DOWN = new AtomicBoolean();

    @TestConfiguration(proxyBeanMethods = false)
    static class StubCatalog {
        @Bean
        @Primary
        BookCatalog stubBookCatalog() {
            return id -> {
                if (CATALOG_DOWN.get()) {
                    throw new CatalogUnavailableException("down", null);
                }
                return !MISSING_BOOK.equals(id);
            };
        }
    }

    @Autowired
    MockMvc mvc;

    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();

    @BeforeEach
    void reset() {
        CATALOG_DOWN.set(false);
    }

    String as(UUID id, String name, String role) {
        return TestJwts.bearer(id, name, role);
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String auth, String body) {
        req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        return auth == null ? req : req.header("Authorization", auth);
    }

    String createBody(UUID book, int rating) {
        return """
                {"bookId":"%s","rating":%d,"body":"Great read"}""".formatted(book, rating);
    }

    String createReview(UUID book, UUID user, String name, int rating) throws Exception {
        String res = mvc.perform(json(post("/api/v1/reviews"), as(user, name, "USER"), createBody(book, rating)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    @Test
    void anonymousCanReadButNotWrite() throws Exception {
        UUID book = UUID.randomUUID();
        String id = createReview(book, alice, "alice", 4);
        mvc.perform(get("/api/v1/reviews")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reviews").param("bookId", book.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/reviews/" + id)).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/reviews"), null, createBody(book, 3))).andExpect(status().isUnauthorized());
    }

    @Test
    void identityComesFromTokenNotBody() throws Exception {
        UUID book = UUID.randomUUID();
        String body = """
                {"bookId":"%s","rating":5,"body":"hi","userId":"%s","accountId":"%s","username":"mallory"}"""
                .formatted(book, bob, bob);
        mvc.perform(json(post("/api/v1/reviews"), as(alice, "alice", "USER"), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(alice.toString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void duplicateIs409AndBadRatingIs400() throws Exception {
        UUID book = UUID.randomUUID();
        createReview(book, alice, "alice", 4);
        mvc.perform(json(post("/api/v1/reviews"), as(alice, "alice", "USER"), createBody(book, 2)))
                .andExpect(status().isConflict());
        for (int bad : new int[] {0, 6}) {
            mvc.perform(json(post("/api/v1/reviews"), as(bob, "bob", "USER"), createBody(book, bad)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void ownershipRules() throws Exception {
        UUID book = UUID.randomUUID();
        String id = createReview(book, alice, "alice", 3);
        String upd = """
                {"rating":5,"body":"changed"}""";

        mvc.perform(json(put("/api/v1/reviews/" + id), as(bob, "bob", "USER"), upd)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reviews/" + id).header("Authorization", as(bob, "bob", "USER")))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/v1/reviews/" + id), as(admin, "root", "ADMIN"), upd))
                .andExpect(status().isForbidden());

        mvc.perform(json(put("/api/v1/reviews/" + id), as(alice, "alice", "USER"), upd))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(5));
        mvc.perform(delete("/api/v1/reviews/" + id).header("Authorization", as(admin, "root", "ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/reviews/" + id)).andExpect(status().isNotFound());

        String id2 = createReview(UUID.randomUUID(), alice, "alice", 3);
        mvc.perform(delete("/api/v1/reviews/" + id2).header("Authorization", as(alice, "alice", "USER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownBookIs422AndCatalogDownIs503() throws Exception {
        mvc.perform(json(post("/api/v1/reviews"), as(alice, "alice", "USER"), createBody(MISSING_BOOK, 3)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Book does not exist"));
        CATALOG_DOWN.set(true);
        mvc.perform(json(post("/api/v1/reviews"), as(alice, "alice", "USER"), createBody(UUID.randomUUID(), 3)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void summaryAggregates() throws Exception {
        UUID book = UUID.randomUUID();
        mvc.perform(get("/api/v1/reviews/books/" + book + "/summary"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));
        createReview(book, alice, "alice", 5);
        createReview(book, bob, "bob", 2);
        var res = mvc.perform(get("/api/v1/reviews/books/" + book + "/summary")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(res, "$.reviewCount")).isEqualTo(2);
        assertThat((Double) JsonPath.read(res, "$.averageRating")).isEqualTo(3.5);
        assertThat((String) JsonPath.read(res, "$.bookId")).isEqualTo(book.toString());
    }
}
