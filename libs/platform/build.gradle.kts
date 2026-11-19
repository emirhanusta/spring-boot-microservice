plugins { `java-platform` }

javaPlatform { allowDependencies() }

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(platform(libs.spring.cloud.dependencies))
    api(platform(libs.testcontainers.bom))
}
