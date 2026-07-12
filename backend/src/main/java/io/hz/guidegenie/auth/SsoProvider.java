package io.hz.guidegenie.auth;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 런타임에 관리되는 SSO(OIDC) provider. 로그인(OAuth2 Client)과 API 인증(Resource Server)
 * 양쪽에서 이 테이블을 참조해 동적으로 구성된다.
 */
@Getter
@Entity
@Table(name = "sso_provider")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SsoProvider extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false, unique = true)
    private String registrationId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "issuer_uri", nullable = false)
    private String issuerUri;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    /** AES-256 암호화된 client secret. */
    @Column(name = "encrypted_client_secret")
    private String encryptedClientSecret;

    /** 콤마 구분 스코프. 예) openid,profile,email */
    @Column(nullable = false)
    private String scopes;

    @Column(nullable = false)
    private boolean enabled;

    public SsoProvider(String registrationId, String displayName, String issuerUri,
                       String clientId, String encryptedClientSecret, String scopes) {
        this.registrationId = registrationId;
        this.displayName = displayName;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.scopes = (scopes == null || scopes.isBlank()) ? "openid,profile,email" : scopes;
        this.enabled = true;
    }

    public void update(String displayName, String issuerUri, String clientId,
                       String encryptedClientSecret, String scopes, boolean enabled) {
        this.displayName = displayName;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        if (encryptedClientSecret != null) {
            this.encryptedClientSecret = encryptedClientSecret;
        }
        if (scopes != null && !scopes.isBlank()) {
            this.scopes = scopes;
        }
        this.enabled = enabled;
    }
}
