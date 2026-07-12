dependencies {
    implementation(project(":common"))
    implementation(project(":domain-rag")) // 동기화 시 수집 문서 색인
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json") // config jsonb
    implementation("org.springframework:spring-web")                    // 외부 커넥터(RestClient)용
}
