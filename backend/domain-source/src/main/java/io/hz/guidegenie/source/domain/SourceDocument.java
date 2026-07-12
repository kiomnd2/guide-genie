package io.hz.guidegenie.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

/** 수집된 원본 문서. source.source_document 매핑. */
@Entity
@Getter
@Table(name = "source_document", schema = "source")
public class SourceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    /** 원본 시스템 식별자(Jira issue key, Confluence page id, GitHub path 등). */
    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    private String url;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    protected SourceDocument() {} // JPA 전용

    public SourceDocument(Long connectionId, String externalId, String title, String content,
                          String url, OffsetDateTime syncedAt) {
        this.connectionId = connectionId;
        this.externalId = externalId;
        this.title = title;
        this.content = content;
        this.url = url;
        this.syncedAt = syncedAt;
    }

    public void update(String title, String content, String url, OffsetDateTime syncedAt) {
        this.title = title;
        this.content = content;
        this.url = url;
        this.syncedAt = syncedAt;
    }
}
