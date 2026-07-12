# guide-genie

AI 사용 가이드 자동생성 서비스 — 프로젝트에 연동된 Jira · Confluence · GitHub 데이터를
기반으로 사용 가이드를 자동 생성하고, 가이드 기반 Q&A(출처 표시)를 제공합니다.

> 기획서: `사용가이드-자동생성-기획서.md` (v0.2)

## 아키텍처

```
[React SPA]  ──REST / SSE──▶  [Spring Boot API Server]
                                 ├─ Auth (Spring Security + SSO[OIDC] + JWT)
                                 ├─ Project / Guide / Q&A API
                                 ├─ Connector (Jira · Confluence · GitHub)
                                 ├─ Sync Scheduler
                                 └─ AI Orchestrator (Spring AI)
                                        ├─ Gemini 2.5 Flash (생성/답변)
                                        └─ Gemini Embedding
             [PostgreSQL + pgvector]   [Redis(캐시/세션)]
```

## 기술 스택

| 계층 | 기술 |
|---|---|
| Frontend | React 18, TypeScript, Vite, react-markdown, CodeMirror |
| Backend | Java 21, Spring Boot 3.x, Spring AI |
| LLM / Embedding | Gemini 2.5 Flash / Gemini Embedding (Vertex AI) |
| DB | PostgreSQL + pgvector 단일 구성 |
| 캐시/세션 | Redis |
| 인증 | 사내 SSO(OIDC) + Spring Security + JWT |

## 디렉토리 구조

```
guide-genie/
├── backend/     Spring Boot API 서버
├── frontend/    React SPA
├── docker-compose.yml   Postgres(pgvector) · Redis
└── README.md
```

## 로컬 실행

### 1. 인프라 (Postgres + Redis)

```bash
docker compose up -d
```

Postgres는 `pgvector/pgvector:pg16` 이미지를 사용해 `vector` 확장이 활성화됩니다.

### 2. 백엔드

```bash
cd backend
./gradlew bootRun
# http://localhost:8080  (Swagger: /swagger-ui.html)
```

환경변수(또는 `application-local.yml`)로 다음을 설정하세요:

```
GOOGLE_CLOUD_PROJECT       Vertex AI 프로젝트 ID
GOOGLE_CLOUD_LOCATION      예) us-central1
DB_URL / DB_USERNAME / DB_PASSWORD
TOKEN_ENC_KEY              토큰/시크릿 암호화 키(AES-256, 32바이트)
```

> SSO는 `application.yml`에 고정하지 않고 **DB에서 동적으로 관리**합니다. 아래 참조.

### 3. 프론트엔드

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173  (→ /api 는 8080으로 프록시)
```

## 동적 SSO(OIDC) 설정

SSO provider를 `application.yml`에 고정하지 않고 **DB(`sso_provider`)에서 런타임에 추가/수정**합니다.
변경은 재배포 없이 즉시 로그인·API 인증에 반영됩니다.

- **로그인(OAuth2 Client)**: `DynamicClientRegistrationRepository` — `registrationId`로 DB 조회 후
  issuer의 OIDC discovery로 `ClientRegistration`을 구성(캐시).
- **API 인증(Resource Server)**: `DynamicJwtAuthenticationManagerResolver` — 토큰의 `iss`를
  DB 신뢰 목록과 대조해 issuer별 JWT 검증기를 선택(캐시). 목록 밖 issuer는 거부.
- 캐시는 provider 변경 시 자동 무효화됩니다.

### 관리 API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/admin/sso-providers` | provider 등록 |
| GET | `/api/admin/sso-providers` | 목록(시크릿 제외) |
| PUT | `/api/admin/sso-providers/{id}` | 수정(secret 미입력 시 기존 유지) |
| DELETE | `/api/admin/sso-providers/{id}` | 삭제 |

등록 예시:

```json
{
  "registrationId": "corp-sso",
  "displayName": "사내 SSO",
  "issuerUri": "https://sso.example.com",
  "clientId": "guide-genie",
  "clientSecret": "••••••",
  "scopes": "openid,profile,email"
}
```

client secret은 AES-256으로 암호화 저장되며 응답에 절대 노출되지 않습니다.

### 최초 부트스트랩 (chicken-and-egg)

관리 API도 인증이 필요하므로, **첫 provider는 DB에 직접 삽입**해 부트스트랩합니다:

```sql
INSERT INTO sso_provider (registration_id, display_name, issuer_uri, client_id, scopes, enabled)
VALUES ('corp-sso', '사내 SSO', 'https://sso.example.com', 'guide-genie', 'openid,profile,email', true);
```

> secret이 필요한 로그인 흐름이면 `encrypted_client_secret`에 `TokenCipher`로 암호화한 값을 넣거나,
> 등록 후 `PUT`으로 갱신하세요. 운영에서는 `/api/admin/**`를 관리자 롤로 제한(`@PreAuthorize`)하는 것을 권장합니다.

## 구현 현황

스캐폴드 단계입니다. 도메인 모델 · REST 컨트롤러 · 커넥터 인터페이스 · AI 오케스트레이터
골격이 준비되어 있으며, 실제 로직은 각 서비스의 `// TODO` 로 표시되어 있습니다.

로드맵(M1~M4)은 기획서 9장을 참고하세요.
