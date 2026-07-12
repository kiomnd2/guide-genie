package io.hz.guidegenie.guide;

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
import org.springframework.data.annotation.CreatedDate;

@Getter
@Entity
@Table(name = "guide_revision")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public GuideRevision(Long guideId, String contentMd, int version, String editedBy) {
        this.guideId = guideId;
        this.contentMd = contentMd;
        this.version = version;
        this.editedBy = editedBy;
    }
}
