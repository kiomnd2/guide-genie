-- 동적 SSO provider. application.yml 고정 대신 런타임 등록/수정.
CREATE TABLE sso_provider (
    id                      BIGSERIAL PRIMARY KEY,
    registration_id         VARCHAR(100) NOT NULL UNIQUE,   -- OAuth2 registrationId (로그인 경로 식별자)
    display_name            VARCHAR(200),
    issuer_uri              VARCHAR(500) NOT NULL,          -- OIDC issuer (.well-known discovery 기준)
    client_id               VARCHAR(200) NOT NULL,
    encrypted_client_secret TEXT,                           -- AES-256 암호화 저장
    scopes                  VARCHAR(500) NOT NULL DEFAULT 'openid,profile,email',
    enabled                 BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sso_provider_issuer ON sso_provider(issuer_uri);
CREATE INDEX idx_sso_provider_enabled ON sso_provider(enabled);
