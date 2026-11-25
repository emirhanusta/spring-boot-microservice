package com.readscape.security.itest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.readscape.security.CurrentUser;
import com.readscape.testfixtures.TestJwtDecoderConfig;
import com.readscape.testfixtures.TestJwts;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = SecurityAutoConfigurationTest.App.class,
        properties = "readscape.security.public-paths=GET /open/**, /anything")
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class SecurityAutoConfigurationTest {

    @SpringBootApplication
    @Import(Endpoints.class)
    static class App {}

    @RestController
    static class Endpoints {
        @GetMapping("/secured")
        String secured() {
            return CurrentUser.userId() + "|" + CurrentUser.username() + "|" + CurrentUser.isAdmin();
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "ok";
        }

        @GetMapping("/open/hello")
        String open() {
            return "hi";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }
    }

    @Autowired
    MockMvc mvc;

    private final UUID id = UUID.randomUUID();

    @Test
    void noTokenIs401ProblemJson() throws Exception {
        mvc.perform(get("/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validTokenIs200AndCurrentUserWorks() throws Exception {
        mvc.perform(get("/secured").header("Authorization", TestJwts.bearer(id, "alice", "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(id + "|alice|false"));
    }

    @Test
    void userOnAdminEndpointIs403ProblemJson() throws Exception {
        mvc.perform(get("/admin").header("Authorization", TestJwts.bearer(id, "alice", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminOnAdminEndpointIs200() throws Exception {
        mvc.perform(get("/admin").header("Authorization", TestJwts.bearer(id, "root", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void wrongIssuerIs401() throws Exception {
        String token = TestJwts.token(id, "alice", "someone-else", Duration.ofMinutes(5), List.of("USER"));
        mvc.perform(get("/secured").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        String token = TestJwts.token(id, "alice", TestJwts.ISSUER, Duration.ofMinutes(-5), List.of("USER"));
        mvc.perform(get("/secured").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void publicPathsAndHealthNeedNoToken() throws Exception {
        mvc.perform(get("/open/hello")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(post("/open/hello")).andExpect(status().isUnauthorized());
    }
}
