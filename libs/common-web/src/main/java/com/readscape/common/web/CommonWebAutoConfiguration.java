package com.readscape.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registers the shared problem-detail advice in servlet applications. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProblemDetailExceptionHandler.class)
    ProblemDetailExceptionHandler problemDetailExceptionHandler() {
        return new ProblemDetailExceptionHandler();
    }
}
