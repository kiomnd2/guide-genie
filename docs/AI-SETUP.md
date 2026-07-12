# AI 셋업 가이드 (Spring AI · Gemini)

`docs/AI-INTEGRATION.md`가 설명한 RAG 파이프라인의 **미구현 3지점**(임베딩 · 벡터검색 · LLM 호출)을
실제로 켜는 실전 런북. 코드 배선은 이미 포트로 연결돼 있으므로 각 지점만 채우면 된다.

> **인증 방식 2가지** — 코드(§3·§4)는 동일하고 **의존성/설정만 다르다** (Spring AI가 provider를 추상화).
>
> | 방식 | 준비물 | 적합 |
> |---|---|---|
> | **A. Gemini API 키 (권장)** | Google AI Studio API 키 1개 | 개발·내부·소규모 |
> | B. Vertex AI | GCP 프로젝트 + 결제 + 자격증명(ADC) | 조직 거버넌스·데이터 레지던시 |
>
> 대상 파일: `domain-rag/EmbeddingService`(색인/검색), `domain-guide/GuideGenerationService` ·
> `domain-qna/QnaService`(LLM), `app-api`/`app-worker`의 `application.yml`.

---

## 0. 준비물

**방식 A (Gemini API 키 — 권장)**
1. https://aistudio.google.com → **Get API key** → 키 발급 (GCP 프로젝트/결제 설정 불필요, 무료 티어 있음)
2. 환경변수: `GEMINI_API_KEY=<발급한 키>`

**방식 B (Vertex AI)**
- GCP 프로젝트 + Vertex AI API 활성화(`gcloud services enable aiplatform.googleapis.com`)
- 자격증명(ADC): `gcloud auth application-default login` 또는 서비스계정 키 → `GOOGLE_APPLICATION_CREDENTIALS`
- `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION=us-central1`

> pgvector는 두 방식 공통으로 이미 준비됨(`pgvector/pgvector:pg16` + Flyway `CREATE EXTENSION vector`).

---

## 1. 의존성 (`backend/domain-rag/build.gradle.kts`)

**방식 A — OpenAI 스타터를 Gemini의 OpenAI 호환 엔드포인트에 연결**
```kotlin
dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json")

    implementation("org.springframework.ai:spring-ai-starter-model-openai") // Gemini(OpenAI 호환)
}
```

**방식 B — Vertex AI 스타터**
```kotlin
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-embedding")
```

루트 `backend/build.gradle.kts` `dependencyManagement`에 Spring AI BOM 추가(공통):
```kotlin
imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    mavenBom("org.springframework.ai:spring-ai-bom:1.0.0") // GA
}
```
> LLM 호출을 하는 `domain-guide`·`domain-qna`도 같은 스타터가 필요하다. 권장: **`domain-rag`에 `GenerationPort`를
> 두고 ChatModel 호출을 한 곳에 모으면** provider 의존이 `domain-rag`에만 남는다(현재 Search/IndexPort와 동일 패턴).
> ⚠️ 아티팩트 이름은 Spring AI 버전마다 다르다(GA `spring-ai-starter-*` vs 마일스톤 `spring-ai-*-spring-boot-starter`). start.spring.io로 확인.

---

## 2. 설정 (프로파일 게이팅)

자격증명/키가 없어도 로컬이 부팅되도록 AI 자동설정은 `ai` 프로파일에서만 켠다.

**방식 A** — `backend/app-api/src/main/resources/application-ai.yml` (신규)
```yaml
spring:
  ai:
    openai:
      api-key: ${GEMINI_API_KEY}
      base-url: https://generativelanguage.googleapis.com/v1beta/openai
      chat:
        options:
          model: gemini-2.5-flash
          temperature: 0.2               # 환각 억제(기획 10장)
      embedding:
        options:
          model: text-embedding-004      # 768차원 → 스키마 vector(768)와 일치
```

**방식 B** — `application-ai.yml`
```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${GOOGLE_CLOUD_PROJECT}
          location: ${GOOGLE_CLOUD_LOCATION:us-central1}
          chat.options: { model: gemini-2.5-flash, temperature: 0.2 }
        embedding:
          project-id: ${GOOGLE_CLOUD_PROJECT}
          location: ${GOOGLE_CLOUD_LOCATION:us-central1}
          text.options: { model: text-embedding-004 }
```

기본(비 `ai`) 프로파일에서는 해당 자동설정을 제외해 키 없이 부팅되게 한다. `application.yml`:
```yaml
spring:
  autoconfigure:
    exclude:
      # 방식 A
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      # 방식 B (Vertex 사용 시)
      # - org.springframework.ai.model.vertexai.autoconfigure.gemini.VertexAiGeminiChatAutoConfiguration
      # - org.springframework.ai.model.vertexai.autoconfigure.embedding.VertexAiTextEmbeddingAutoConfiguration
```
> exclude FQCN은 버전에 따라 다르다 — 부팅 로그의 자동설정 리포트로 정확한 이름 확인.

실행:
```bash
# 방식 A
GEMINI_API_KEY=xxxxx ./gradlew :app-api:bootRun --args='--spring.profiles.active=ai'
# 방식 B
GOOGLE_CLOUD_PROJECT=my-proj ./gradlew :app-api:bootRun --args='--spring.profiles.active=ai'
```

---

## 3. 임베딩 · 벡터검색 구현 (`domain-rag/EmbeddingService`)

우리 스키마(`rag.embedding_chunk`, `project_id`+`metadata`+`embedding vector(768)`)를 그대로 쓰므로
Spring AI `VectorStore`(자체 테이블)가 아니라 **`EmbeddingModel`(provider 무관) + pgvector 네이티브 쿼리**를 쓴다.

`EmbeddingModel` + `JdbcTemplate` 주입 후 TODO를 채운다:
```java
// index(): 청크 저장 직후 벡터 계산·저장
float[] vec = embeddingModel.embed(chunkText);
jdbc.update("UPDATE rag.embedding_chunk SET embedding = CAST(? AS vector) WHERE id = ?",
    toVectorLiteral(vec), savedChunk.getId());

// search(): 질의 임베딩 → 코사인 유사도 상위 K (project 범위)
float[] q = embeddingModel.embed(query);
return jdbc.query("""
    SELECT chunk_text, metadata, 1 - (embedding <=> CAST(? AS vector)) AS score
    FROM rag.embedding_chunk
    WHERE project_id = ? AND embedding IS NOT NULL
    ORDER BY embedding <=> CAST(? AS vector) LIMIT ?
    """,
    (rs, i) -> new RetrievedChunk(rs.getString("chunk_text"),
        parseJson(rs.getString("metadata")), rs.getDouble("score")),
    toVectorLiteral(q), projectId, toVectorLiteral(q), topK);

static String toVectorLiteral(float[] v) { return Arrays.toString(v).replace(" ", ""); }
```
- `embedding` 컬럼은 JPA 미매핑 → 위처럼 네이티브로 다룬다.
- 대량 색인은 `embed(List<String>)` 배치 + 임베딩 캐시로 비용 절감.

---

## 4. LLM 호출 구현 (생성 · Q&A)

`GuideGenerationService`·`QnaService`의 TODO에 `ChatModel`(provider 무관) 연결:
```java
// 가이드 생성
String md = chatModel.call("""
    아래 컨텍스트만 근거로 '%s' 가이드를 Markdown으로 작성. 컨텍스트에 없으면 추측 금지.
    [요청] %s
    [컨텍스트] %s
    """.formatted(title, prompt, join(context)));

// Q&A (근거 없으면 미응답은 이미 처리됨). 스트리밍은 StreamingChatModel.stream(...) → SSE
```
- 온도 낮게(≈0.2), 출처는 검색 청크 metadata로 구성.

---

## 5. 검증

```bash
# ai 프로파일로 기동 (방식 A 예시)
GEMINI_API_KEY=xxxxx ./gradlew :app-api:bootRun --args='--spring.profiles.active=ai'

# 가이드 게시/동기화 후 색인 벡터가 채워지는지
docker exec guide-genie-postgres psql -U guidegenie -d guidegenie -c \
  "select count(*) from rag.embedding_chunk where embedding is not null;"

# Q&A (실제 답변 + 출처)
curl -N -X POST localhost:8080/api/projects/1/qna \
  -H 'Content-Type: application/json' -H 'Accept: text/event-stream' \
  -d '{"question":"로컬 세팅 방법?"}'
```

---

## 6. 체크리스트 · 함정

- [ ] 방식 A: `GEMINI_API_KEY` 설정 + `ai` 프로파일. 방식 B: `GOOGLE_CLOUD_PROJECT` + 자격증명.
- [ ] 기본 프로파일은 AI 자동설정 exclude로 키 없이도 부팅되게 유지.
- [ ] **임베딩 차원 일치**: `text-embedding-004`=768 ↔ 스키마 `vector(768)`. 모델 바꾸면 컬럼 차원 마이그레이션 필요.
- [ ] Gemini API 키 방식은 **무료 티어 rate limit**이 낮다 — 대량 동기화 시 배치/백오프.
- [ ] 검색은 항상 `project_id` 스코프. 재색인은 `IndexPort.index()`가 멱등 처리(기존 청크 삭제 후 재삽입).
- [ ] exclude FQCN / 스타터 아티팩트명은 Spring AI 버전에 맞춰 확인.

---

관련: `docs/AI-INTEGRATION.md`(설계), `docs/ARCHITECTURE.md`(모듈/포트), `CLAUDE.md`.
