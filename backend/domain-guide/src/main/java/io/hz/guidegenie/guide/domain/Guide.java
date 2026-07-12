package io.hz.guidegenie.guide.domain;

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
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 가이드 도메인 모델. guide.guide 매핑.
 * 프로젝트/분류는 ID로만 참조(크로스 스키마 FK 없음, 도메인 경계 규칙).
 */
@Entity
@Getter
@Table(name = "guide", schema = "guide")
public class Guide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    /** 소속 분류(중분류 또는 대분류). NULL = 미분류. */
    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuideStatus status;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /** 자동 생성 시 프롬프트/참조 소스/모델 버전 등. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_meta", columnDefinition = "jsonb")
    private Map<String, Object> generationMeta;

    protected Guide() {} // JPA 전용

    public Guide(Long projectId, String title, Long categoryId, String createdBy,
                 Map<String, Object> generationMeta) {
        this.projectId = projectId;
        this.title = title;
        this.categoryId = categoryId;
        this.status = GuideStatus.DRAFT;
        this.createdBy = createdBy;
        this.generationMeta = generationMeta;
    }

    public void edit(String title, Long categoryId) {
        this.title = title;
        this.categoryId = categoryId;
    }

    public void moveToCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void publish() {
        this.status = GuideStatus.PUBLISHED;
    }
}
