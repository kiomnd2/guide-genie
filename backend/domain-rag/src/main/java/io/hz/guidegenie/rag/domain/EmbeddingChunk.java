package io.hz.guidegenie.rag.domain;

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
 * RAG 검색용 임베딩 청크. rag.embedding_chunk 매핑.
 *
 * <p>{@code embedding vector(768)} 컬럼은 pgvector 도입 시 네이티브 쿼리로 다루며 JPA에는 매핑하지 않는다.
 * 출처 메타데이터(guide_id, source_url, title, anchor …)와 원문 청크만 매핑한다.
 */
@Entity
@Getter
@Table(name = "embedding_chunk", schema = "rag")
public class EmbeddingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    private RefType refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String chunkText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected EmbeddingChunk() {} // JPA 전용

    public EmbeddingChunk(Long projectId, RefType refType, Long refId, String chunkText,
                          Map<String, Object> metadata) {
        this.projectId = projectId;
        this.refType = refType;
        this.refId = refId;
        this.chunkText = chunkText;
        this.metadata = metadata;
    }
}
