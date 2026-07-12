# 아키텍처 컨벤션 — 헥사고날 (Level 1, 실용형)

| 항목 | 내용 |
|------|------|
| 결정 | 모듈러 모놀리스 위에 **각 `domain-*` 모듈 내부를 포트&어댑터(헥사고날) 패키지 구조**로 |
| 깊이 | **Level 1 (실용)** — 포트/어댑터는 도입, JPA 엔티티를 도메인 모델로 겸용(매퍼 없음) |
| 레퍼런스 | signallink 프로젝트 컨벤션을 따름 |

---

## 1. 모듈 = 도메인 경계

Gradle 모듈 의존으로 방향을 강제한다(역참조는 컴파일 에러).

```
common ← 모두
domain-project → common
domain-rag     → common
domain-guide   → common, domain-rag
domain-source  → common, domain-rag
domain-qna     → common, domain-rag
app-api        → common + 모든 domain-*
app-worker     → common, domain-source
```

- 레이어별 모듈 분리는 하지 않는다(도메인별 모듈만).
- 도메인 간 데이터 참조는 **ID로만**. 크로스 스키마 FK 없음. 타 도메인 기능이 필요하면 그 도메인의 **포트**를 호출한다(예: guide/source/qna → `domain-rag`의 `IndexPort`/`SearchPort`).
- `domain-rag`는 색인/검색을 제공하는 **foundational** 모듈(다른 도메인이 downward 의존).

## 2. 도메인 모듈 내부 표준 패키지 (`io.hz.guidegenie.<domain>`)

```
├─ domain/                도메인 모델(@Entity 겸용) + 도메인 규칙
├─ application/
│  ├─ port/in/            인바운드 포트(노출 유스케이스)     (선택)
│  ├─ port/out/           아웃바운드 포트(Repository·Gateway) (필수)
│  └─ service/            유스케이스 구현(@Service)
└─ adapter/out/
   ├─ persistence/        JpaRepository + <X>PersistenceAdapter (impl out 포트)
   └─ external/           <X>Client (impl gateway 포트)
```

인바운드 어댑터는 app 모듈에:
- REST 컨트롤러 → `app-api` (`io.hz.guidegenie.api.*`)
- 스케줄러 잡 → `app-worker` (`io.hz.guidegenie.worker.*`)

## 3. 의존 규칙

- `domain/`·`application/`은 프레임워크 의존 최소. 외부 기술(DB·HTTP·타 도메인)은 **포트 경유**.
- 프레임워크/외부 라이브러리 의존은 **`adapter/`에만**.
- 유스케이스(`service`)는 구체 어댑터를 모른다 — 포트 인터페이스만 생성자 주입.
- 웹 기술(SSE·HttpServletRequest 등)은 도메인에 두지 않는다 — 컨트롤러(app-api)가 처리하고 서비스는 결과 객체 반환.

## 4. Level 1 절충

- **엔티티 = 도메인 모델 겸용**: `domain/`의 클래스에 JPA 애노테이션 허용(`@Entity`, `@Table(schema=...)`). 순수 POJO + 별도 JpaEntity + 매퍼는 두지 않는다.
- 매퍼 계층 없음. 외부 API 응답 → 도메인 변환은 어댑터 내부 메서드로 간단히.

## 5. 인바운드 포트(port/in)는 선택

- 단순 진입점(컨트롤러가 `@Service`를 직접 호출)은 `port/in` 생략 가능.
- 여러 도메인이 재사용하는 유스케이스는 `port/in` 인터페이스로 노출 — 예: `domain-rag`의 `IndexPort`/`SearchPort`(guide/source/qna가 호출).
- **`port/out`은 항상 필수**(교체성·모킹 이득).

## 6. 네이밍

| 대상 | 접미사 | 예 |
|------|--------|-----|
| 인바운드 포트 | `Port`/`UseCase` | `IndexPort`, `SearchPort` |
| 아웃바운드 포트 | `Port` | `GuideRepositoryPort`, `SourceConnectorPort` |
| 유스케이스 구현 | `Service` | `GuideService`, `SyncService` |
| 영속 어댑터 | `PersistenceAdapter` | `GuidePersistenceAdapter` |
| 외부 API 어댑터 | `Client` | `GitHubClient`, `JiraClient` |

## 7. 스키마 · 감사

- 스키마는 `app-api` Flyway가 소유(`V1__init.sql`). 도메인별 스키마: `project`/`guide`/`source`/`rag`/`qna`. `ddl-auto: none`.
- 엔티티는 `@Table(name=..., schema=...)`로 소속 스키마 명시.
- 감사 필드는 `common.BaseTimeEntity`(+`@EnableJpaAuditing` in app-api). `OffsetDateTime` 공급용 커스텀 `DateTimeProvider` 필수.
