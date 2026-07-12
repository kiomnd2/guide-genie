package io.hz.guidegenie.guide.domain;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 가이드 세트 템플릿(프로젝트별). guide.guide_template 매핑. */
@Entity
@Getter
@Table(name = "guide_template", schema = "guide")
public class GuideTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<TemplateItem> items;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    protected GuideTemplate() {} // JPA 전용

    public GuideTemplate(Long projectId, String name, List<TemplateItem> items, String createdBy) {
        this.projectId = projectId;
        this.name = name;
        this.items = items == null ? List.of() : items;
        this.createdBy = createdBy;
    }

    public void update(String name, List<TemplateItem> items) {
        this.name = name;
        this.items = items == null ? List.of() : items;
    }
}
