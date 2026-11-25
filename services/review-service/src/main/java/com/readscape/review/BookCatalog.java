package com.readscape.review;

import java.util.UUID;

/** Book existence check. Temporary REST lookup; M2 replaces it with an event-fed projection. */
public interface BookCatalog {

    /**
     * @return true if the book exists, false if the catalog says 404
     * @throws CatalogUnavailableException if the catalog cannot answer (unreachable, timeout, 5xx)
     */
    boolean exists(UUID bookId);
}
