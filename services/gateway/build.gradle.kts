plugins { id("readscape.spring-service") }

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    // Reactive resource server: validates a Bearer token when one is present (RS256 via JWKS).
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    // springdoc 3.0.x targets Spring Boot 4.0.x (version intentionally local to this service)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3")

    testImplementation(project(":libs:test-fixtures"))
}
