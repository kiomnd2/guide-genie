package io.hz.guidegenie.rag.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hz.guidegenie.rag.application.port.in.IndexPort;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import io.hz.guidegenie.rag.application.port.out.EmbeddingChunkRepositoryPort;
import io.hz.guidegenie.rag.domain.EmbeddingChunk;
import io.hz.guidegenie.rag.domain.RefType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RAG 색인(IndexPort) + 검색(SearchPort) 유스케이스.
 *
 * <p>'ai' 프로파일이면 Spring AI {@link EmbeddingModel}로 벡터를 계산해 pgvector에 저장/검색한다.
 * AI 미설정(모델 빈 없음)이면 청크·메타데이터만 저장하고 검색은 빈 결과를 반환한다(stub 폴백).
 * {@code embedding vector(768)} 컬럼은 JPA 미매핑 → 네이티브 SQL로 다룬다.
 */
@Slf4j
@Service
public class EmbeddingService implements IndexPort, SearchPort {

    private final EmbeddingChunkRepositoryPort repository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel; // AI 미설정 시 null
    private final TextChunker chunker;

    public EmbeddingService(EmbeddingChunkRepositoryPort repository,
                            JdbcTemplate jdbc,
                            ObjectMapper objectMapper,
                            ObjectProvider<EmbeddingModel> embeddingModelProvider,
                            @Value("${guidegenie.rag.chunk-size}") int chunkSize,
                            @Value("${guidegenie.rag.chunk-overlap}") int overlap) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModelProvider.getIfAvailable();
        this.chunker = new TextChunker(chunkSize, overlap);
    }

    private boolean aiEnabled() {
        return embeddingModel != null;
    }

    @Override
    @Transactional
    public void index(Long projectId, RefType refType, Long refId, String title, String text) {
        repository.deleteByRef(refType, refId);
        List<String> chunks = chunker.split(text);
        for (String chunkText : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ref_type", refType.name());
            metadata.put("title", title);
            if (refType == RefType.GUIDE) {
                metadata.put("guide_id", refId);
            }
            EmbeddingChunk saved =
                repository.save(new EmbeddingChunk(projectId, refType, refId, chunkText, metadata));

            if (aiEnabled()) {
                float[] vec = embeddingModel.embed(chunkText);
                jdbc.update("UPDATE rag.embedding_chunk SET embedding = CAST(? AS vector) WHERE id = ?",
                    toVectorLiteral(vec), saved.getId());
            }
        }
        log.debug("[RAG] index {} #{} → {} chunks (ai={})", refType, refId, chunks.size(), aiEnabled());
    }

    @Override
    @Transactional
    public void removeByRef(RefType refType, Long refId) {
        repository.deleteByRef(refType, refId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> search(Long projectId, String query, int topK) {
        if (!aiEnabled()) {
            log.debug("[RAG] search skipped (ai disabled) projectId={}", projectId);
            return List.of();
        }
        String vec = toVectorLiteral(embeddingModel.embed(query));
        return jdbc.query("""
                SELECT chunk_text, metadata, 1 - (embedding <=> CAST(? AS vector)) AS score
                FROM rag.embedding_chunk
                WHERE project_id = ? AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
            (rs, i) -> new RetrievedChunk(
                rs.getString("chunk_text"), readJson(rs.getString("metadata")), rs.getDouble("score")),
            vec, projectId, vec, topK);
    }

    private static String toVectorLiteral(float[] v) {
        return Arrays.toString(v).replace(" ", "");
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
