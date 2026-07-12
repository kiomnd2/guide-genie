plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain-source")) // 동기화 유스케이스 트리거
    implementation(project(":domain-rag"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // Flyway 없음 — 스키마는 app-api가 소유. 워커는 동일 DB에 ddl-auto=none 으로 접속.
}
