package com.readscape.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailExceptionHandlerTest {

    record Payload(@NotBlank String name) {}

    @RestController
    static class TestController {
        @GetMapping("/missing")
        String missing() {
            throw NotFoundException.of("Book", 42);
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @PostMapping("/items")
        String create(@Valid @RequestBody Payload payload) {
            return payload.name();
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ProblemDetailExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        mvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Book with id '42' was not found"));
    }

    @Test
    void validationFailureMapsTo400WithFieldErrors() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void unexpectedErrorMapsTo500WithoutLeakingInternals() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
}
