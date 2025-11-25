plugins {
    java
    id("org.springframework.boot") version "3.2.11"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.mbotamapay"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")
    // runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Security & JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Jackson Java 8 date/time module (fix for LocalDateTime serialization)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Monitoring
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // QR Code
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.zxing:javase:3.5.2")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // PDF Generation
    implementation("com.github.librepdf:openpdf:1.3.30")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("commons-codec:commons-codec:1.16.0")

    // Spring Cloud OpenFeign for payment provider integrations
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.0")
    
    // JSON processing for metadata
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct - Re-enabled with Java 21 compatible configuration
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    
    // Property-Based Testing
    testImplementation("net.jqwik:jqwik:1.8.2")
    
    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    
    // WireMock for mocking external APIs
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
