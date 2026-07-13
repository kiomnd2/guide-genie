# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

**guide-genie** — 프로젝트에 연동된 Jira · Confluence · GitHub 데이터를 기반으로 **사용 가이드를 자동 생성**하고, 가이드 기반 **Q&A(출처 표시)** 를 제공하는 서비스. 기획서: `사용가이드-자동생성-기획서.md`.

- **백엔드**: Java 21 · Spring Boot 3.4.1 · Gradle 멀티모듈(Kotlin DSL) · PostgreSQL + Flyway (`backend/`)
- **프론트엔드**: React 18 · TypeScript · Vite (`frontend/`)
- **LLM/RAG**: Gemini 2.5 Flash + pgvector (예정 — 현재 스캐폴드, `domain-rag`의 `// TODO`)

> 참조 문서 (새 작업 전 확인):
> - `docs/ARCHITECTURE.md` — 모듈러 모놀리스 + 헥사고날 컨벤션
> - `docs/AI-INTEGRATION.md` — RAG 파이프라인(수집→색인→검색→생성), 포트 연결, 미구현 TODO
> - `docs/AI-SETUP.md` — Spring AI(Vertex Gemini + pgvector) 실전 셋업 런북(TODO 3지점 구현)

## 빌드 · 테스트 · 실행

Gradle 루트는 **`backend/`** (프론트는 별도 npm 프로젝트).

```bash
# 인프라 (Postgres + pgvector)
docker compose up -d

# 백엔드 (API 서버 :8080, Swagger /swagger-ui.html)
cd backend
./gradlew build                       # 전체 빌드
./gradlew :domain-guide:build         # 단일 모듈
./gradlew :app-api:bootRun            # API 서버 실행
./gradlew :app-worker:bootRun         # 동기화 워커(비웹 데몬) 실행

# 프론트엔드 (:5173, /api → 8080 프록시)
cd frontend && npm install && npm run dev
```

- 인증은 **아직 미도입** — 모든 `/api/**` 는 인증 없이 접근 가능하고 감사 필드(created_by 등)는 `anonymous`.
- 테스트 프레임워크는 JUnit 5 + AssertJ 전제(루트 `build.gradle.kts`가 `junit-platform-launcher` 주입). 현재 백엔드 테스트는 미작성 상태(스캐폴드).

## 아키텍처 — 모듈러 모놀리스 + 헥사고날 (Level 1)

**Gradle 모듈 = 도메인 경계.** 의존 방향은 모듈 의존 선언으로 강제되어 역참조는 컴파일 에러:

```
common ← 모두가 의존
domain-project → common
domain-rag     → common
domain-guide   → common, domain-rag       (게시/수정 시 색인, AI 생성 시 검색)
domain-source  → common, domain-rag       (동기화 시 수집 문서 색인)
domain-qna     → common, domain-rag       (게시 가이드 검색)
app-api        → common + 모든 domain-*    (REST 컨트롤러 + Flyway 소유)
app-worker     → common, domain-source     (증분 동기화 스케줄러; 비웹)
```

- **도메인 간 참조는 ID로만** — 크로스 스키마 FK 없음. 예: `guide.guide.project_id`, `guide.guide.category_id`(같은 스키마라 FK 有), `rag.embedding_chunk.project_id`.
- **`domain-rag`는 foundational** — 색인/검색을 인바운드 포트(`IndexPort`·`SearchPort`)로 노출하고, 상위 도메인(guide/source/qna)이 downward 로 호출한다.

**각 `domain-*` 모듈 내부는 포트&어댑터 패키지 구조** (`io.hz.guidegenie.<domain>`):

```
io.hz.guidegenie.<domain>
├─ domain/                엔티티(= 도메인 모델 겸용, @Entity 허용) + 도메인 규칙
├─ application/
│  ├─ port/in/            인바운드 포트(유스케이스/노출 포트) — 선택 (rag는 IndexPort/SearchPort로 사용)
│  ├─ port/out/           아웃바운드 포트(Repository·Gateway) — 필수
│  └─ service/            유스케이스 구현(@Service, 포트에만 의존)
└─ adapter/out/{persistence,external}   Spring Data JPA / 외부 API 어댑터(포트 구현)
```

- **Level 1 = 실용형**: JPA 엔티티를 도메인 모델로 겸용(별도 매퍼 없음). 엔티티는 `protected` 무인자 생성자 + 전체 생성자, Lombok `@Getter`.
- **인바운드 어댑터는 app 모듈에**: REST 컨트롤러 → `app-api`(`io.hz.guidegenie.api.*`), 스케줄러 잡 → `app-worker`(`io.hz.guidegenie.worker.*`). 웹 기술(SSE 등)은 컨트롤러에 두고 도메인 서비스는 결과 객체만 반환한다(예: `QnaService.answer` → `QnaAnswer`, SSE는 `QnaController`).
- **네이밍**: `*UseCase`(in 포트) / `*Port`(out 포트) / `*Service`(유스케이스) / `*PersistenceAdapter` / `*Client`(외부 API).
- 생성자 주입은 Lombok `@RequiredArgsConstructor` + `private final`. 수동 작성 금지.

### 스키마 소유 · 감사

- DB 스키마는 **`app-api`의 Flyway가 소유**(`app-api/src/main/resources/db/migration/V1__init.sql`). 도메인별 Postgres 스키마: `project` · `guide` · `source` · `rag` · `qna`. `ddl-auto: none`.
- `app-worker`는 같은 DB에 `flyway.enabled=false`, `ddl-auto=none`로 접속(스키마는 api가 소유).
- 감사(@CreatedDate/@LastModifiedDate)는 `common.BaseTimeEntity` + `app-api`의 `JpaConfig(@EnableJpaAuditing)`. 필드 타입이 `OffsetDateTime`이라 커스텀 `DateTimeProvider`로 공급(기본 provider는 LocalDateTime 반환 → 변환 실패).

## 프론트엔드 (`frontend/`)

- 라우팅: 대시보드(프로젝트 목록) → **프로젝트 컨텍스트**(사이드바 = `가이드 / 소스 연동 / Q&A`). projectId는 URL(`/projects/:id/...`)에서 잡는다.
- 가이드: 목록은 **대분류 › 중분류 목차 트리** + **드래그 앤 드롭 이동**(HTML5 DnD, `PATCH /guides/{id}/category`). 워크스페이스는 `[읽기]/[편집]` 2탭.
- API 클라이언트 `src/api/client.ts`. 응답 형태는 프론트 계약이므로 컨트롤러 DTO 형태를 함부로 바꾸지 말 것.

## 협업 · 컨벤션

- 브랜치→PR (`main` 직접 push 금지). 커밋 메시지는 gitmoji + 한국어 제목.
- **문서 동기화**: 백엔드 구조/스키마 변경 시 `CLAUDE.md`·`docs/ARCHITECTURE.md`를 코드에 맞춰 갱신.
- 새 도메인 모듈 추가 시: `settings.gradle.kts`에 include → `<module>/build.gradle.kts` 작성(common + 필요한 domain 의존) → 위 포트&어댑터 패키지 구조 준수 → 엔티티에 스키마 지정(`@Table(schema=...)`) → Flyway에 스키마/테이블 추가.

## 함정 (겪은 것)

- **스키마 변경 시 dev DB 리셋 필요**: Flyway V1을 바꾸면 기존 적용 이력과 체크섬이 어긋난다. dev에서는 스키마 drop 후 재기동. 운영에서는 V2+ 증분 마이그레이션으로만 변경.
- **JPA 감사 + OffsetDateTime**: `JpaConfig`의 커스텀 `DateTimeProvider` 없으면 저장 시 `Cannot convert LocalDateTime to OffsetDateTime`. 독립 `@CreatedDate` 엔티티(GuideRevision·QnaSession·QnaMessage)는 `@EntityListeners(AuditingEntityListener.class)`를 각자 붙여야 채워진다.
- **Spring AI 미도입**: 임베딩 벡터 계산/검색과 LLM 호출은 `domain-rag`·`domain-guide`·`domain-qna`의 `// TODO`. 붙일 때 Spring AI(Vertex AI Gemini + pgvector VectorStore)를 `domain-rag`에 추가하고, project-id 미설정 시 부팅 실패하므로 자동설정을 프로파일로 게이팅할 것.
- **`@Async` 자기호출**: `SyncService.syncAllIncremental` → `this.sync`는 자기호출이라 비동기로 안 돈다(스케줄러에선 순차 실행 의도라 무방). API 경로(`SourceConnectionService.create` → `syncService.sync`)는 빈 경계를 넘으므로 정상 비동기.
- **커넥터 구현 현황**: **GitHub `*Client`는 구현됨** — README·파일 구조·의미 있는 파일 본문(문서>설정>소스 순, `guidegenie.github.max-files`/`max-file-bytes`로 상한)을 수집해 RAG 색인 → 상세 가이드 근거로 사용. **Jira/Confluence `*Client`는 아직 스텁**(`fetchAll`이 빈 리스트 반환, `// TODO`).
