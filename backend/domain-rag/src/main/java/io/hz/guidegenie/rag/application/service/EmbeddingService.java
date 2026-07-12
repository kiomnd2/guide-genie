package io.hz.guidegenie.rag.application.service;

import io.hz.guidegenie.rag.application.port.in.IndexPort;
import io.hz.guidegenie.rag.application.port.in.RetrievedChunk;
import io.hz.guidegenie.rag.application.port.in.SearchPort;
import io.hz.guidegenie.rag.application.port.out.EmbeddingChunkRepositoryPort;
import io.hz.guidegenie.rag.domain.EmbeddingChunk;
import io.hz.guidegenie.rag.domain.RefType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RAG 유스케이스 — 색인(IndexPort)과 검색(SearchPort) 인바운드 포트를 구현한다.
 *
 * <p>스캐폴드: 청크 분할과 메타데이터 저장은 동작한다. 실제 임베딩 벡터 계산/저장과
 * 벡터 유사도 검색은 Spring AI EmbeddingModel + pgvector 네이티브 쿼리로 채운다(TODO).
 */
@Slf4j
@Service
public class EmbeddingService implements IndexPort, SearchPort {

    private final EmbeddingChunkRepositoryPort repository;
    private final TextChunker chunker;

    public EmbeddingService(EmbeddingChunkRepositoryPort repository,
                            @Value("${guidegenie.rag.chunk-size}") int chunkSize,
                            @Value("${guidegenie.rag.chunk-overlap}") int overlap) {
        this.repository = repository;
        this.chunker = new TextChunker(chunkSize, overlap);
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
            repository.save(new EmbeddingChunk(projectId, refType, refId, chunkText, metadata));
            // TODO: EmbeddingModel.embed(chunkText) → embedding_chunk.embedding 업데이트(native)
        }
        log.debug("[RAG] index {} #{} → {} chunks", refType, refId, chunks.size());
    }

    @Override
    @Transactional
    public void removeByRef(RefType refType, Long refId) {
        repository.deleteByRef(refType, refId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> search(Long projectId, String query, int topK) {
        log.debug("[RAG] search projectId={} topK={} query={}", projectId, topK, query);
        // TODO: EmbeddingModel.embed(query) → SELECT ... ORDER BY embedding <=> :vec LIMIT topK (pgvector)
        return List.of();
    }
}
