package io.hz.guidegenie.guide.domain;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 가이드 분류. 2단계 — 대분류(parentId==null) > 중분류(parentId=대분류). guide.category 매핑.
 */
@Entity
@Getter
@Table(name = "category", schema = "guide")
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** NULL이면 대분류. */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Category() {} // JPA 전용

    public Category(Long projectId, Long parentId, String name) {
        this.projectId = projectId;
        this.parentId = parentId;
        this.name = name;
        this.sortOrder = 0;
    }

    public boolean isMajor() {
        return parentId == null;
    }

    public void rename(String name) {
        this.name = name;
    }
}
