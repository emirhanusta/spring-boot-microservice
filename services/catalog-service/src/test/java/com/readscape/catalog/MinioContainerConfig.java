package com.readscape.catalog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/** MinIO container wired into {@code readscape.storage.*}. */
@TestConfiguration(proxyBeanMethods = false)
class MinioContainerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    MinIOContainer minio() {
        return new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z@sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e").asCompatibleSubstituteFor("minio/minio"));
    }

    @Bean
    DynamicPropertyRegistrar minioProperties(MinIOContainer minio) {
        return registry -> {
            registry.add("readscape.storage.endpoint", minio::getS3URL);
            registry.add("readscape.storage.access-key", minio::getUserName);
            registry.add("readscape.storage.secret-key", minio::getPassword);
        };
    }
}
