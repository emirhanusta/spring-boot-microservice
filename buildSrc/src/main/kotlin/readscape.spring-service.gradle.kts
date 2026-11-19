import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("readscape.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("readscape/${project.name}")
}
