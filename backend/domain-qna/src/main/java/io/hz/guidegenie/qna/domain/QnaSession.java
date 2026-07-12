package io.hz.guidegenie.qna.domain;

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

/** Q&A 세션. qna.qna_session 매핑. */
@Entity
@Getter
@Table(name = "qna_session", schema = "qna")
@EntityListeners(AuditingEntityListener.class)
public class QnaSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected QnaSession() {} // JPA 전용

    public QnaSession(Long projectId, String userId) {
        this.projectId = projectId;
        this.userId = userId;
    }
}
