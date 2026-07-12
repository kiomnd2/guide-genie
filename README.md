# guide-genie

AI 사용 가이드 자동생성 서비스 — 프로젝트에 연동된 Jira · Confluence · GitHub 데이터를
기반으로 사용 가이드를 자동 생성하고, 가이드 기반 Q&A(출처 표시)를 제공합니다.

> 기획서: `사용가이드-자동생성-기획서.md` (v0.2)

## 아키텍처

```
[React SPA]  ──REST / SSE──▶  [Spring Boot API Server]
                                 ├─ Project / Guide / Q&A API
                                 ├─ Connector (Jira · Confluence · GitHub)
                                 ├─ Sync Scheduler
                                 └─ AI Orchestrator (Spring AI)
                                        ├─ Gemini 2.5 Flash (생성/답변)
                                        └─ Gemini Embedding
             [PostgreSQL + pgvector]   [Redis(캐시)]

* 인증(SSO/JWT)은 현재 미도입 — 모든 API는 인증 없이 접근 가능(수동 등록 위주).
```

## 기술 스택

| 계층 | 기술 |
|---|---|
| Frontend | React 18, TypeScript, Vite, react-markdown, CodeMirror |
| Backend | Java 21, Spring Boot 3.4.1, Gradle 멀티모듈(Kotlin DSL) |
| 아키텍처 | 모듈러 모놀리스 + 헥사고날(Level 1) — `docs/ARCHITECTURE.md` |
| LLM / Embedding | Gemini 2.5 Flash / pgvector (예정, 현재 스캐폴드) |
| DB | PostgreSQL + pgvector |
| 인증 | 미도입 (추후 사내 SSO(OIDC) 연동 예정) |

## 디렉토리 구조

```
guide-genie/
├── backend/       Gradle 멀티모듈 (루트)
│   ├── common/            공통(BaseTimeEntity, TokenCipher …)
│   ├── domain-project/    프로젝트
│   ├── domain-rag/        임베딩/검색 (IndexPort·SearchPort)
│   ├── domain-guide/      가이드 + 리비전 + 분류 + AI 생성
│   ├── domain-source/     소스 연동 + 수집 + 커넥터
│   ├── domain-qna/        Q&A
│   ├── app-api/           REST + Flyway (스키마 소유)
│   └── app-worker/        동기화 스케줄러(비웹)
├── frontend/      React SPA
├── docs/ARCHITECTURE.md
├── CLAUDE.md
└── docker-compose.yml
```

## 로컬 실행

### 1. 인프라 (Postgres + pgvector)

```bash
docker compose up -d
```

Postgres는 `pgvector/pgvector:pg16` 이미지를 사용해 `vector` 확장이 활성화됩니다.

### 2. 백엔드 (Gradle 루트 = `backend/`)

```bash
cd backend
./gradlew :app-api:bootRun        # API 서버 :8080 (Swagger: /swagger-ui.html)
./gradlew :app-worker:bootRun     # (선택) 동기화 워커
```

> 인증은 아직 없습니다. 모든 API에 인증 없이 접근할 수 있고, 감사 필드(created_by 등)는 `anonymous`로 기록됩니다.

환경변수로 다음을 설정할 수 있습니다(모두 기본값 있음):

```
DB_URL / DB_USERNAME / DB_PASSWORD
TOKEN_ENC_KEY              소스 연동 토큰 암호화 키(AES-256, 32바이트)
GOOGLE_CLOUD_PROJECT       Vertex AI 프로젝트 ID (AI 기능 사용 시)
GOOGLE_CLOUD_LOCATION      예) us-central1 (AI 기능 사용 시)
```

### 3. 프론트엔드

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173  (→ /api 는 8080으로 프록시)
```

## 구현 현황

스캐폴드 단계입니다. 도메인 모델 · REST 컨트롤러 · 커넥터 인터페이스 · AI 오케스트레이터
골격이 준비되어 있으며, 실제 로직은 각 서비스의 `// TODO` 로 표시되어 있습니다.

로드맵(M1~M4)은 기획서 9장을 참고하세요.
