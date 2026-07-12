package io.hz.guidegenie.project.domain;

import io.hz.guidegenie.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 프로젝트 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용). project.project 매핑. */
@Entity
@Getter
@Table(name = "project", schema = "project")
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String owner;

    protected Project() {} // JPA 전용

    public Project(String name, String description, String owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
