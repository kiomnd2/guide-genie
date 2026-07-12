-- 가이드 세트 템플릿(프로젝트별). items = [{title, prompt, categoryId}] jsonb.
CREATE TABLE guide.guide_template (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name       VARCHAR(300) NOT NULL,
    items      JSONB NOT NULL DEFAULT '[]',
    created_by VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_guide_template_project ON guide.guide_template(project_id);
