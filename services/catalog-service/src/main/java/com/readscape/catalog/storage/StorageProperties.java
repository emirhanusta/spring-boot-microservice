package com.readscape.catalog.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("readscape.storage")
public record StorageProperties(String endpoint, String region, String bucket, String accessKey, String secretKey) {}
