# AI 연동 매커니즘 (RAG 파이프라인)

guide-genie의 AI는 **RAG(검색 증강 생성)** 파이프라인이다. 소스/가이드를 잘게 쪼개 임베딩해두고(색인),
질문·프롬프트가 오면 관련 조각을 찾아(검색) LLM 프롬프트에 넣어 답을 만든다(생성).

> 현재 상태: **배선(포트/흐름)은 완성**, 실제 벡터 계산·벡터 검색·LLM 호출 3지점은 `// TODO`(Spring AI 도입 시 채움).

---

## 1. 전체 흐름

```
①수집             ②색인(임베딩)              ③검색(RAG)                ④생성(LLM)
Jira/GitHub/Conf ─► 청크분할 + 임베딩 ─────► pgvector 유사도 검색 ─────► Gemini ─► 가이드 초안
가이드 게시/수정 ─►  rag.embedding_chunk                                        └► Q&A 답변 + 출처
```

| 단계 | 위치(모듈) | 핵심 클래스 | 현재 상태 |
|---|---|---|---|
| ① 수집 | domain-source | `SyncService`, `*Client`(Jira/Confluence/GitHub) | 배선 O / 커넥터 fetch stub |
| ② 색인 | domain-rag | `EmbeddingService.index()`, `TextChunker` | 청크·메타데이터 저장 O / 벡터 계산 TODO |
| ③ 검색 | domain-rag | `EmbeddingService.search()` (`SearchPort`) | 인터페이스 O / 벡터 검색 TODO(빈 리스트) |
| ④ 생성 | domain-guide, domain-qna | `GuideGenerationService`, `QnaService` | 흐름 O / LLM 호출 TODO(stub 응답) |

---

## 2. 단계별 상세

### ① 수집 (domain-source)

커넥터가 원본 시스템에서 문서를 가져와 `SourceDocument`로 upsert하고, 곧바로 색인을 요청한다.

- `SyncService.sync(connectionId, full)`
  - `ConnectorRegistry.forType(type)` 로 타입별 커넥터 선택
  - `connector.fetchAll()` 또는 `fetchSince(lastSyncedAt)` (전체/증분)
  - 문서 upsert(`external_id` 기준) → `ragIndex.index(projectId, RefType.SOURCE, docId, title, content)`
- 커넥터는 아웃바운드 포트 `SourceConnectorPort`의 구현(`adapter/out/external/*Client`). 현재 `fetch*`는 빈 리스트 반환 stub.
- 증분 동기화 스케줄은 `app-worker`의 `SyncScheduler`(`@Scheduled`) → `SyncService.syncAllIncremental()`.

### ② 색인 = 임베딩 (domain-rag)

- `EmbeddingService.index(projectId, refType, refId, title, text)`
  1. `removeByRef(refType, refId)` — 재색인 전 기존 청크 제거(멱등)
  2. `TextChunker.split(text)` — 단어 근사 청크 분할 (설정: `guidegenie.rag.chunk-size=800`, `chunk-overlap=100`)
  3. 청크마다 `EmbeddingChunk` 저장: `project_id`, `ref_type`, `ref_id`, `chunk_text`, **`metadata`(jsonb)**
- `rag.embedding_chunk`에는 `embedding vector(768)` 컬럼이 있으나 **JPA에 매핑하지 않음** — pgvector 네이티브로 다룰 예정. 현재 NULL.
- 가이드도 색인 대상: `GuideService`가 저장/게시 시 `ragIndex.index(projectId, RefType.GUIDE, guideId, title, contentMd)` 호출.

**metadata 규약** (출처 추적의 근거):

| 키 | 의미 |
|---|---|
| `ref_type` | `GUIDE` / `SOURCE` |
| `title` | 가이드 제목 / 문서 제목 |
| `guide_id` | (GUIDE일 때) 가이드 id |
| `anchor` | (예정) 섹션 헤딩 앵커 |
| `source_url` | (예정) 원본 링크 |

### ③ 검색 (domain-rag)

- `SearchPort.search(projectId, query, topK)` → `List<RetrievedChunk>` (설정: `guidegenie.rag.top-k=6`)
- **TODO**: 질의 임베딩 → pgvector 코사인 유사도
  ```sql
  SELECT chunk_text, metadata
  FROM rag.embedding_chunk
  WHERE project_id = :pid
  ORDER BY embedding <=> :query_vec
  LIMIT :topK;
  ```
  벡터 유사도 + 키워드(BM25) 하이브리드 권장(기획서 4.2). 현재는 `List.of()` 반환.

### ④ 생성 (domain-guide / domain-qna)

- **가이드 자동생성** — `GuideGenerationService.generate(projectId, prompt, author, jobId)` `@Async`
  - `ragSearch.search(projectId, prompt, topK)` 로 컨텍스트 확보
  - **TODO**: `chatModel.call(promptTemplate(prompt, context))` → Markdown
  - `guideService.createDraft(...)` 로 **DRAFT** 저장(생성 이력 `generation_meta`에 프롬프트·모델·참조 청크 수 기록)
  - 비동기이므로 컨트롤러는 `jobId`만 즉시 반환(`POST /guides/generate` → 202)
- **Q&A** — `QnaService.answer(projectId, session, question)`
  - USER 메시지 저장 → `ragSearch.search()`
  - 결과 없으면 `"가이드에서 답을 찾을 수 없습니다"`(**환각 방지**, 근거 없으면 미응답)
  - 있으면 검색 청크로 컨텍스트 구성 → **TODO** LLM 답변 → 청크 metadata로 `Citation` 조립 → ASSISTANT 메시지 저장 → `QnaAnswer` 반환
  - **SSE 스트리밍은 어댑터(app-api)** 담당: `QnaController`가 `token`/`citations`/`done` 이벤트로 전송(웹 기술을 도메인 밖으로 유지)

---

## 3. 도메인 연결 = 포트 (헥사고날)

`domain-rag`가 색인/검색을 **인바운드 포트**로 노출하고, 상위 도메인이 아래로(downward) 호출한다.
모듈 의존으로 방향이 강제되므로 상위 도메인은 임베딩 "구현"을 모르고 포트에만 의존한다 → Spring AI로 교체해도 상위 코드 불변.

```
domain-guide ─┐
domain-source ─┼──► domain-rag
domain-qna   ─┘      ├─ application/port/in/IndexPort   : index() / removeByRef()   (색인)
                     └─ application/port/in/SearchPort   : search()                  (검색)
                     구현: application/service/EmbeddingService
```

| 호출자 | 사용 포트 | 시점 |
|---|---|---|
| `GuideService` | `IndexPort` | 가이드 저장/게시 |
| `SyncService` | `IndexPort` | 소스 동기화 |
| `GuideGenerationService` | `SearchPort` | AI 가이드 생성 |
| `QnaService` | `SearchPort` | Q&A 질의 |

---

## 4. 출처(Citation)

색인 시 청크 `metadata`에 `guide_id`·`title`·`source_url`·`anchor`를 심어둔다.
답변 시 **검색된 청크의 metadata를 그대로 `Citation`으로 변환**해 반환한다(프론트 Q&A의 출처 카드).
근거가 검색된 청크이므로 "가이드에 없으면 미응답" 원칙이 자연히 성립한다.

```
RetrievedChunk.metadata → Citation(guideId, guideTitle, section, sourceUrl)
```

---

## 5. 미구현(TODO)과 구현 계획

채워야 할 3지점(모두 포트 뒤에 격리되어 있음):

1. **임베딩 벡터** — `EmbeddingService.index()`에서 `EmbeddingModel.embed(chunk)` → `rag.embedding_chunk.embedding` 저장
2. **벡터 검색** — `EmbeddingService.search()`에서 질의 임베딩 + pgvector `<=>` 조회
3. **LLM 호출** — `GuideGenerationService`·`QnaService`에 `ChatModel`(스트리밍은 `StreamingChatModel`) 주입

도입 절차:

1. `domain-rag/build.gradle.kts`에 Spring AI 의존 추가
   - `spring-ai-vertex-ai-gemini-spring-boot-starter` (Chat: Gemini 2.5 Flash)
   - `spring-ai-vertex-ai-embedding-spring-boot-starter` (Embedding)
   - `spring-ai-pgvector-store-spring-boot-starter` 또는 pgvector 네이티브 쿼리
2. `app-api`/`app-worker` `application.yml`에 Vertex 설정(`spring.ai.vertex.ai.*`) + `GOOGLE_CLOUD_PROJECT`/`GOOGLE_CLOUD_LOCATION`
3. 위 3개 `// TODO` 구현
4. 임베딩 차원(현재 스키마 `vector(768)`)을 실제 모델 출력 차원에 맞춰 조정

### ⚠️ 함정 (겪은 것)

- Vertex AI 자동설정은 **부팅 시점에 `project-id`를 요구**한다. 미설정이면 앱이 뜨지 않음.
  → AI 자동설정을 `ai` 같은 **프로파일로 게이팅**하고, 자격증명 없이도 로컬 부팅이 되게 유지한다. (`CLAUDE.md` 함정 참조)
- 검색 스코프는 `project_id`로 제한한다(프로젝트 간 유출 방지).

---

## 6. 관련 설정 (application.yml)

```yaml
guidegenie:
  rag:
    chunk-size: 800      # 청크 크기(단어 근사)
    chunk-overlap: 100   # 오버랩
    top-k: 6             # 검색 상위 K
```

기획서 근거: `사용가이드-자동생성-기획서.md` 2장(F2/F4), 4.2(RAG 파이프라인·출처 추적).
