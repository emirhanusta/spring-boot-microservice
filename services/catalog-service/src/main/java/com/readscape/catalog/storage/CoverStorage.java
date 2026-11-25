package com.readscape.catalog.storage;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/** Object storage for book covers. */
public interface CoverStorage {

    record StoredCover(InputStream content, String contentType, long contentLength) {}

    static String keyFor(UUID bookId) {
        return "books/" + bookId + "/cover";
    }

    void put(String key, byte[] data, String contentType);

    /** The caller must close the returned stream. */
    Optional<StoredCover> get(String key);

    /** Idempotent. */
    void delete(String key);
}
