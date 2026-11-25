package com.readscape.catalog.storage;

import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
class S3CoverStorage implements CoverStorage {

    private final S3Client s3;
    private final String bucket;
    private volatile boolean bucketReady;

    S3CoverStorage(S3Client s3, StorageProperties props) {
        this.s3 = s3;
        this.bucket = props.bucket();
    }

    @Override
    public void put(String key, byte[] data, String contentType) {
        ensureBucket();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(data));
    }

    @Override
    public Optional<StoredCover> get(String key) {
        try {
            var in = s3.getObject(b -> b.bucket(bucket).key(key));
            var r = in.response();
            return Optional.of(new StoredCover(in, r.contentType(), r.contentLength()));
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (NoSuchBucketException ignored) {
            // nothing stored yet
        }
    }

    /** Creates the bucket on first write so the service starts even when object storage is not up yet. */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            s3.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3.createBucket(b -> b.bucket(bucket));
        }
        bucketReady = true;
    }
}
