package com.readscape.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jwt.SignedJWT;
import com.readscape.testfixtures.PostgresContainerConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class IdentityFlowTest {

    @Autowired
    MockMvc mvc;

    private static String unique() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Performs the request and asserts that no password field / hash ever appears in a response. */
    private MvcResult run(MockHttpServletRequestBuilder req) throws Exception {
        MvcResult result = mvc.perform(req).andReturn();
        String body = result.getResponse().getContentAsString().toLowerCase();
        assertThat(body).doesNotContain("\"password").doesNotContain("passwordhash").doesNotContain("password_hash")
                .doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("$2y$");
        return result;
    }

    private MvcResult json(MockHttpServletRequestBuilder req, String body) throws Exception {
        return run(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private MvcResult register(String username, String extra) throws Exception {
        return json(post("/api/v1/auth/register"),
                """
                {"username":"%s","email":"%s@example.com","password":"s3cret-pass"%s}""".formatted(username, username, extra));
    }

    private String login(String username, String password) throws Exception {
        MvcResult r = json(post("/api/v1/auth/login"), """
                {"username":"%s","password":"%s"}""".formatted(username, password));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String name = unique();
        MvcResult reg = register(name, "");
        assertThat(reg.getResponse().getStatus()).isEqualTo(201);
        String id = JsonPath.read(reg.getResponse().getContentAsString(), "$.id");

        MvcResult loginResult = json(post("/api/v1/auth/login"), """
                {"username":"%s","password":"s3cret-pass"}""".formatted(name));
        String body = loginResult.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(body, "$.tokenType")).isEqualTo("Bearer");
        assertThat((Integer) JsonPath.read(body, "$.expiresIn")).isEqualTo(900);
        String token = JsonPath.read(body, "$.accessToken");

        MvcResult me = run(get("/api/v1/users/me").header("Authorization", "Bearer " + token));
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.getResponse().getContentAsString(), "$.id")).isEqualTo(id);
        assertThat((String) JsonPath.read(me.getResponse().getContentAsString(), "$.username")).isEqualTo(name);

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("readscape-identity");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(id);
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("USER");
    }

    @Test
    void registerIgnoresRequestedRole() throws Exception {
        MvcResult reg = register(unique(), ",\"role\":\"ADMIN\"");
        assertThat(reg.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(reg.getResponse().getContentAsString(), "$.role")).isEqualTo("USER");
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        String name = unique();
        assertThat(register(name, "").getResponse().getStatus()).isEqualTo(201);
        assertThat(register(name, "").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void wrongPasswordAndUnknownUserLookIdentical() throws Exception {
        String name = unique();
        register(name, "");
        MvcResult wrong = json(post("/api/v1/auth/login"), """
                {"username":"%s","password":"nope-nope"}""".formatted(name));
        MvcResult unknown = json(post("/api/v1/auth/login"), """
                {"username":"%s","password":"nope-nope"}""".formatted(unique()));
        assertThat(wrong.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        assertThat(wrong.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());
    }

    @Test
    void userCannotReadOthersButAdminCan() throws Exception {
        String alice = unique();
        String bob = unique();
        String aliceId = JsonPath.read(register(alice, "").getResponse().getContentAsString(), "$.id");
        register(bob, "");
        String bobToken = login(bob, "s3cret-pass");
        String adminToken = login("admin", "admin");

        assertThat(run(get("/api/v1/users/" + aliceId).header("Authorization", "Bearer " + bobToken))
                        .getResponse().getStatus()).isEqualTo(403);
        MvcResult asAdmin = run(get("/api/v1/users/" + aliceId).header("Authorization", "Bearer " + adminToken));
        assertThat(asAdmin.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(asAdmin.getResponse().getContentAsString(), "$.username")).isEqualTo(alice);
    }

    @Test
    void listIsAdminOnlyAndPaged() throws Exception {
        String name = unique();
        register(name, "");
        String userToken = login(name, "s3cret-pass");
        assertThat(run(get("/api/v1/users").header("Authorization", "Bearer " + userToken)).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(run(get("/api/v1/users")).getResponse().getStatus()).isEqualTo(401);

        MvcResult page = run(get("/api/v1/users?size=1").header("Authorization", "Bearer " + login("admin", "admin")));
        assertThat(page.getResponse().getStatus()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(page.getResponse().getContentAsString(), "$.content.length()")).isEqualTo(1);
    }

    @Test
    void updateEmailAndDeleteSelf() throws Exception {
        String name = unique();
        String id = JsonPath.read(register(name, "").getResponse().getContentAsString(), "$.id");
        String token = login(name, "s3cret-pass");

        MvcResult updated = json(put("/api/v1/users/me").header("Authorization", "Bearer " + token),
                """
                {"email":"New-%s@Example.com"}""".formatted(name));
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(updated.getResponse().getContentAsString(), "$.email"))
                .isEqualTo("new-" + name + "@example.com");

        assertThat(run(delete("/api/v1/users/" + id).header("Authorization", "Bearer " + token))
                        .getResponse().getStatus()).isEqualTo(204);
        mvc.perform(get("/api/v1/users/" + id).header("Authorization", "Bearer " + login("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void jwksKeyMatchesTokenKid() throws Exception {
        String name = unique();
        register(name, "");
        String token = login(name, "s3cret-pass");
        String kid = SignedJWT.parse(token).getHeader().getKeyID();
        assertThat(SignedJWT.parse(token).getHeader().getAlgorithm().getName()).isEqualTo("RS256");

        String jwks = run(get("/.well-known/jwks.json")).getResponse().getContentAsString();
        assertThat((String) JsonPath.read(jwks, "$.keys[0].kid")).isEqualTo(kid);
        assertThat((String) JsonPath.read(jwks, "$.keys[0].kty")).isEqualTo("RSA");
        assertThat(jwks).doesNotContain("\"d\"").doesNotContain("\"p\"").doesNotContain("\"q\"");
    }

    @Test
    void invalidRegistrationIs400() throws Exception {
        // (not via run(): the field-level error legitimately names the password field)
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"email\":\"bad\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        assertThat(mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
