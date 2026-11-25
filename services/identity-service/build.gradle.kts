plugins { id("readscape.spring-service") }

dependencies {
    implementation(project(":libs:common-web"))
    implementation(project(":libs:security"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // springdoc 3.0.x targets Spring Boot 4.0.x (version intentionally local to this service)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":libs:test-fixtures"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

// Run from the repo root so the dev profile finds .local/keys (see `make dev-keys`).
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}
