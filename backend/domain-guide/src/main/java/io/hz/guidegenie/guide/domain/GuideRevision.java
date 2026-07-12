package io.hz.guidegenie.guide.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 가이드 리비전(저장 시마다 증가). guide.guide_revision 매핑. */
@Entity
@Getter
@Table(name = "guide_revision", schema = "guide")
@EntityListeners(AuditingEntityListener.class)
public class GuideRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_id", nullable = false)
    private Long guideId;

    @Column(name = "content_md", nullable = false, columnDefinition = "text")
    private String contentMd;

    @Column(nullable = false)
    private int version;

    @Column(name = "edited_by", nullable = false)
    private String editedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected GuideRevision() {} // JPA 전용

    public GuideRevision(Long guideId, String contentMd, int version, String editedBy) {
        this.guideId = guideId;
        this.contentMd = contentMd;
        this.version = version;
        this.editedBy = editedBy;
    }
}
