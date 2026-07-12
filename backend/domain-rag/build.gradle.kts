dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json") // metadata jsonb 매핑
    // TODO(RAG): 실제 임베딩/벡터 검색 도입 시 Spring AI (Vertex AI Gemini + pgvector VectorStore) 의존 추가
}
