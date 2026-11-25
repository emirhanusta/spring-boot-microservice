package com.readscape.review;

import java.util.UUID;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

class RestBookCatalog implements BookCatalog {

    private final CatalogApi api;

    RestBookCatalog(CatalogApi api) {
        this.api = api;
    }

    @Override
    public boolean exists(UUID bookId) {
        try {
            api.getBook(bookId);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            // 5xx, other 4xx, connect/read timeouts (ResourceAccessException)
            throw new CatalogUnavailableException("Catalog service is unavailable", e);
        }
    }
}
