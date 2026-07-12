plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain-project"))
    implementation(project(":domain-rag"))
    implementation(project(":domain-guide"))
    implementation(project(":domain-source"))
    implementation(project(":domain-qna"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 스키마 소유: Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // API 문서
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
}
