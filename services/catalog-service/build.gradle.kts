plugins { id("readscape.spring-service") }

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.59.9"))

    implementation(project(":libs:common-web"))
    implementation(project(":libs:security"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("software.amazon.awssdk:s3")
    runtimeOnly("software.amazon.awssdk:url-connection-client")
    // springdoc 3.0.x targets Spring Boot 4.0.x (version intentionally local to this service)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":libs:test-fixtures"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.testcontainers:testcontainers-minio")
}
