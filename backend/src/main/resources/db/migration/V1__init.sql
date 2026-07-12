-- pgvector 확장
CREATE EXTENSION IF NOT EXISTS vector;

-- 프로젝트
CREATE TABLE project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    owner       VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 소스 연동 (Jira / Confluence / GitHub)
CREATE TABLE source_connection (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    type           VARCHAR(20) NOT NULL,           -- JIRA | CONFLUENCE | GITHUB
    config         JSONB NOT NULL,                 -- base_url, workspace/repo, filters 등
    encrypted_token TEXT,                          -- AES-256 암호화된 토큰
    last_synced_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_source_connection_project ON source_connection(project_id);

-- 수집된 원본 문서
CREATE TABLE source_document (
    id            BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL REFERENCES source_connection(id) ON DELETE CASCADE,
    external_id   VARCHAR(500) NOT NULL,           -- 원본 시스템의 식별자
    title         VARCHAR(1000),
    content       TEXT,
    url           VARCHAR(1000),
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (connection_id, external_id)
);
CREATE INDEX idx_source_document_connection ON source_document(connection_id);

-- 가이드
CREATE TABLE guide (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT | PUBLISHED
    created_by      VARCHAR(200) NOT NULL,
    generation_meta JSONB,                          -- prompt, referenced sources, model version
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_guide_project ON guide(project_id);

-- 가이드 리비전
CREATE TABLE guide_revision (
    id         BIGSERIAL PRIMARY KEY,
    guide_id   BIGINT NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
    content_md TEXT NOT NULL,
    version    INT NOT NULL,
    edited_by  VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (guide_id, version)
);

-- 임베딩 청크 (가이드/원본 공용). 차원은 embedding 모델에 맞춰 조정.
CREATE TABLE embedding_chunk (
    id         BIGSERIAL PRIMARY KEY,
    ref_type   VARCHAR(20) NOT NULL,               -- GUIDE | SOURCE
    ref_id     BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding  vector(768),
    metadata   JSONB,                               -- guide_id, heading anchor, source_type, source_url
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_embedding_chunk_ref ON embedding_chunk(ref_type, ref_id);
CREATE INDEX idx_embedding_chunk_vec ON embedding_chunk
    USING hnsw (embedding vector_cosine_ops);

-- Q&A 세션 / 메시지
CREATE TABLE qna_session (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id    VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qna_session_project ON qna_session(project_id);

CREATE TABLE qna_message (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES qna_session(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,               -- USER | ASSISTANT
    content    TEXT NOT NULL,
    citations  JSONB,                               -- [{guideId, section, url}]
    feedback   VARCHAR(10),                         -- UP | DOWN | null
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qna_message_session ON qna_message(session_id);
