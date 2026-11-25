package com.readscape.review;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/** HTTP interface onto catalog-service; the body is ignored, only the status matters. */
interface CatalogApi {

    @GetExchange("/api/v1/books/{id}")
    void getBook(@PathVariable UUID id);
}
