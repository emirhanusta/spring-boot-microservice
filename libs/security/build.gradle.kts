plugins { id("readscape.java-conventions") }

dependencies {
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation(project(":libs:test-fixtures"))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
