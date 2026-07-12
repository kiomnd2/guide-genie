-- 도메인별 스키마 (모듈 경계). 크로스 스키마 FK 없음 — 도메인 간 참조는 ID로만.
CREATE SCHEMA IF NOT EXISTS project;
CREATE SCHEMA IF NOT EXISTS guide;
CREATE SCHEMA IF NOT EXISTS source;
CREATE SCHEMA IF NOT EXISTS rag;
CREATE SCHEMA IF NOT EXISTS qna;

-- pgvector (public 에 설치 → 검색 경로로 vector 타입 해석)
CREATE EXTENSION IF NOT EXISTS vector;

-- ── project ──────────────────────────────────────────────
CREATE TABLE project.project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    owner       VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── guide ────────────────────────────────────────────────
CREATE TABLE guide.category (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,                                  -- project.project (ID 참조, FK 없음)
    parent_id  BIGINT REFERENCES guide.category(id) ON DELETE CASCADE,  -- NULL = 대분류
    name       VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_category_project ON guide.category(project_id);
CREATE INDEX idx_category_parent ON guide.category(parent_id);

CREATE TABLE guide.guide (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    title           VARCHAR(500) NOT NULL,
    category_id     BIGINT REFERENCES guide.category(id) ON DELETE SET NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by      VARCHAR(200) NOT NULL,
    generation_meta JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_guide_project ON guide.guide(project_id);
CREATE INDEX idx_guide_category ON guide.guide(category_id);

CREATE TABLE guide.guide_revision (
    id         BIGSERIAL PRIMARY KEY,
    guide_id   BIGINT NOT NULL REFERENCES guide.guide(id) ON DELETE CASCADE,
    content_md TEXT NOT NULL,
    version    INT NOT NULL,
    edited_by  VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (guide_id, version)
);

-- ── source ───────────────────────────────────────────────
CREATE TABLE source.source_connection (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    type            VARCHAR(20) NOT NULL,           -- JIRA | CONFLUENCE | GITHUB
    config          JSONB NOT NULL,
    encrypted_token TEXT,
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_source_connection_project ON source.source_connection(project_id);

CREATE TABLE source.source_document (
    id            BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL REFERENCES source.source_connection(id) ON DELETE CASCADE,
    external_id   VARCHAR(500) NOT NULL,
    title         VARCHAR(1000),
    content       TEXT,
    url           VARCHAR(1000),
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (connection_id, external_id)
);

-- ── rag ──────────────────────────────────────────────────
CREATE TABLE rag.embedding_chunk (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    ref_type   VARCHAR(20) NOT NULL,               -- GUIDE | SOURCE
    ref_id     BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding  vector(768),                        -- pgvector 도입 시 채움
    metadata   JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_embedding_chunk_ref ON rag.embedding_chunk(ref_type, ref_id);
CREATE INDEX idx_embedding_chunk_project ON rag.embedding_chunk(project_id);
CREATE INDEX idx_embedding_chunk_vec ON rag.embedding_chunk
    USING hnsw (embedding vector_cosine_ops);

-- ── qna ──────────────────────────────────────────────────
CREATE TABLE qna.qna_session (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id    VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qna_session_project ON qna.qna_session(project_id);

CREATE TABLE qna.qna_message (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES qna.qna_session(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,               -- USER | ASSISTANT
    content    TEXT NOT NULL,
    citations  JSONB,
    feedback   VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qna_message_session ON qna.qna_message(session_id);
