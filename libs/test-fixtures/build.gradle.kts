plugins { id("readscape.java-conventions") }

dependencies {
    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:testcontainers-junit-jupiter")
    api("org.testcontainers:testcontainers-postgresql")
    api("org.testcontainers:testcontainers-kafka")
    compileOnly("org.springframework.boot:spring-boot-starter-test")
}
