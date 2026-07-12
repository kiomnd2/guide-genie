package io.hz.guidegenie.embedding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * RAG 검색용 임베딩 청크. {@code embedding vector(768)} 컬럼은 SQL/네이티브 쿼리로 다루며
 * JPA 엔티티에는 매핑하지 않는다(출처 메타데이터/텍스트만 매핑).
 */
@Getter
@Entity
@Table(name = "embedding_chunk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmbeddingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    private RefType refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String chunkText;

    /** guide_id, heading anchor, source_type, source_url 등 출처 추적용 메타데이터. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public EmbeddingChunk(RefType refType, Long refId, String chunkText,
                          Map<String, Object> metadata) {
        this.refType = refType;
        this.refId = refId;
        this.chunkText = chunkText;
        this.metadata = metadata;
    }
}
