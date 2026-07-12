package io.hz.guidegenie.guide;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuideStatus status;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /** 자동 생성 시 사용 프롬프트/참조 소스/모델 버전 등. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_meta", columnDefinition = "jsonb")
    private Map<String, Object> generationMeta;

    public Guide(Long projectId, String title, String createdBy,
                 Map<String, Object> generationMeta) {
        this.projectId = projectId;
        this.title = title;
        this.status = GuideStatus.DRAFT;
        this.createdBy = createdBy;
        this.generationMeta = generationMeta;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void publish() {
        this.status = GuideStatus.PUBLISHED;
    }
}
