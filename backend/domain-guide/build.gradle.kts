dependencies {
    implementation(project(":common"))
    implementation(project(":domain-rag")) // 게시/수정 시 색인, AI 생성 시 검색(RAG 인바운드 포트)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json") // generation_meta jsonb
}
