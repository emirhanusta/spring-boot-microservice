package com.readscape.review;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
class CatalogConfig {

    @Bean
    BookCatalog bookCatalog(@Value("${readscape.catalog.url}") String url) {
        return new RestBookCatalog(catalogApi(url));
    }

    static CatalogApi catalogApi(String url) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        RestClient client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build()
                .createClient(CatalogApi.class);
    }
}
