dependencies {
    implementation(project(":common"))
    implementation(project(":domain-rag")) // 게시 가이드 검색(RAG)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json") // citations jsonb
}
