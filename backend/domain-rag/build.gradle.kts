dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json") // metadata jsonb 매핑

    // Spring AI — Gemini(OpenAI 호환 엔드포인트)용 OpenAI 스타터. ChatModel/EmbeddingModel 제공.
    // 자동설정은 'ai' 프로파일에서만 활성(기본 프로파일은 exclude → 빈 없으면 stub 폴백). docs/AI-SETUP.md
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
}
