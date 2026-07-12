package io.hz.guidegenie.source;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "source_connection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SourceConnection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    /** base_url, workspace/repo, 필터 등. Hibernate가 jsonb로 매핑. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;

    /** AES-256 암호화된 API 토큰 (평문 저장 금지). */
    @Column(name = "encrypted_token")
    private String encryptedToken;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    public SourceConnection(Long projectId, SourceType type, Map<String, Object> config,
                            String encryptedToken) {
        this.projectId = projectId;
        this.type = type;
        this.config = config;
        this.encryptedToken = encryptedToken;
    }

    public void markSynced(OffsetDateTime at) {
        this.lastSyncedAt = at;
    }
}
