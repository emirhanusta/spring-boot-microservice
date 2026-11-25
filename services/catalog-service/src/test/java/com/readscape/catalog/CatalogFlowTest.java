package com.readscape.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.readscape.testfixtures.PostgresContainerConfig;
import com.readscape.testfixtures.TestJwtDecoderConfig;
import com.readscape.testfixtures.TestJwts;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresContainerConfig.class, MinioContainerConfig.class, TestJwtDecoderConfig.class})
class CatalogFlowTest {

    private static final String ADMIN = TestJwts.bearer(UUID.randomUUID(), "root", "ADMIN");
    private static final String USER = TestJwts.bearer(UUID.randomUUID(), "alice", "USER");

    @Autowired
    MockMvc mvc;

    private String createAuthor(String name) throws Exception {
        String body = mvc.perform(post("/api/v1/authors").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createBook(String authorId, String title, String genres) throws Exception {
        String body = mvc.perform(post("/api/v1/books").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":\"%s\",\"title\":\"%s\",\"genres\":%s}".formatted(authorId, title, genres)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void anonymousReadsWorkAndWritesAreProtected() throws Exception {
        mvc.perform(get("/api/v1/authors")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/books")).andExpect(status().isOk());
        String json = "{\"name\":\"X\"}";
        mvc.perform(post("/api/v1/authors").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/authors").header("Authorization", USER)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        String id = createAuthor("Anon " + unique());
        mvc.perform(get("/api/v1/authors/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/authors/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void genresRoundTripAsArrayAndFiltersAndPagingWork() throws Exception {
        String a1 = createAuthor("A1 " + unique());
        String a2 = createAuthor("A2 " + unique());
        String tag = unique();
        String b1 = createBook(a1, "Dune " + tag, "[\"SCIENCE_FICTION\",\"DRAMA\"]");
        createBook(a1, "Emma " + tag, "[\"ROMANCE\"]");
        createBook(a2, "Other " + tag, "[\"ROMANCE\"]");

        mvc.perform(get("/api/v1/books/" + b1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres.length()").value(2))
                .andExpect(jsonPath("$.genres[0]").value("SCIENCE_FICTION"))
                .andExpect(jsonPath("$.genres[1]").value("DRAMA"))
                .andExpect(jsonPath("$.authorId").value(a1))
                .andExpect(jsonPath("$.totalCopies").value(1))
                .andExpect(jsonPath("$.authorName").value(org.hamcrest.Matchers.startsWith("A1 ")));

        mvc.perform(get("/api/v1/books").param("q", "dUnE " + tag))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/books").param("q", tag).param("genre", "ROMANCE"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/books").param("q", tag).param("genre", "ROMANCE").param("authorId", a2))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/books").param("q", tag).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/books").param("genre", "NOPE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/authors/" + a1 + "/books"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deleteAuthorWithBooksConflicts() throws Exception {
        String a = createAuthor("Del " + unique());
        String b = createBook(a, "Book", "[]");
        mvc.perform(delete("/api/v1/authors/" + a).header("Authorization", ADMIN)).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/books/" + b).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/authors/" + a).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/authors/" + a)).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsAreProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/books").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"totalCopies\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='authorId')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='totalCopies')]").exists());
    }

    @Test
    void staleVersionIsConflict() throws Exception {
        String a = createAuthor("Ver " + unique());
        String update = "{\"name\":\"Renamed\",\"version\":%d}";
        mvc.perform(put("/api/v1/authors/" + a).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(update.formatted(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/v1/authors/" + a).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(update.formatted(0)))
                .andExpect(status().isConflict());
    }

    @Test
    void coverLifecycle() throws Exception {
        String a = createAuthor("Cov " + unique());
        String b = createBook(a, "Covered", "[]");
        String url = "/api/v1/books/" + b + "/cover";
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};

        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(upload(url, "image/png", png)).andExpect(status().isUnauthorized());
        mvc.perform(upload(url, "image/png", png).header("Authorization", USER)).andExpect(status().isForbidden());
        mvc.perform(upload(url, "image/png", png).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        var res = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andReturn().getResponse();
        assertThat(res.getContentAsByteArray()).isEqualTo(png);

        mvc.perform(upload(url, "text/plain", png).header("Authorization", ADMIN))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(upload(url, "image/png", new byte[5 * 1024 * 1024 + 1]).header("Authorization", ADMIN))
                .andExpect(status().isContentTooLarge());

        mvc.perform(delete(url).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());

        mvc.perform(upload(url, "image/jpeg", png).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/books/" + b).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    private static MockMultipartHttpServletRequestBuilder upload(String url, String type, byte[] bytes) {
        MockMultipartHttpServletRequestBuilder b = multipart(url).file(new MockMultipartFile("file", "c", type, bytes));
        b.with(r -> {
            r.setMethod("PUT");
            return r;
        });
        return b;
    }
}
