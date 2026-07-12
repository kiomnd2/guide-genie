package io.hz.guidegenie.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "source_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SourceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    /** 원본 시스템 식별자 (Jira issue key, Confluence page id, GitHub path 등). */
    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    private String url;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

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
