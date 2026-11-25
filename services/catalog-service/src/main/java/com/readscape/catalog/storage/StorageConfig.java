package com.readscape.catalog.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(StorageProperties p) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .forcePathStyle(true)
                // S3-compatible stores (MinIO, etc.) don't all accept the SDK's default trailing checksums
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
