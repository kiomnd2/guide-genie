package io.hz.guidegenie.embedding;

import io.hz.guidegenie.source.SourceDocument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정제 텍스트 → 청크 분할 → 임베딩 → pgvector 저장 및 RAG 검색.
 *
 * <p>스캐폴드: 청크 분할과 메타데이터 저장은 동작하며, 실제 임베딩 벡터 계산/저장과
 * 벡터 유사도 검색은 Spring AI {@code EmbeddingModel} + pgvector 네이티브 쿼리로 채운다(TODO).
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingChunkRepository chunkRepository;
    private final TextChunker chunker;

    public EmbeddingService(EmbeddingChunkRepository chunkRepository,
                            @Value("${guidegenie.rag.chunk-size}") int chunkSize,
                            @Value("${guidegenie.rag.chunk-overlap}") int overlap) {
        this.chunkRepository = chunkRepository;
        this.chunker = new TextChunker(chunkSize, overlap);
    }

    @Transactional
    public void embedSourceDocument(SourceDocument doc) {
        chunkRepository.deleteByRef(RefType.SOURCE, doc.getId());
        List<String> chunks = chunker.split(doc.getContent());
        for (String text : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "SOURCE");
            metadata.put("source_url", doc.getUrl());
            metadata.put("title", doc.getTitle());
            EmbeddingChunk chunk = new EmbeddingChunk(RefType.SOURCE, doc.getId(), text, metadata);
            chunkRepository.save(chunk);
            // TODO: EmbeddingModel.embed(text) → embedding_chunk.embedding 업데이트(native query)
        }
        log.debug("[Embedding] source doc {} → {} chunks", doc.getId(), chunks.size());
    }

    @Transactional
    public void embedGuide(Long guideId, String title, String contentMd) {
        chunkRepository.deleteByRef(RefType.GUIDE, guideId);
        List<String> chunks = chunker.split(contentMd);
        for (String text : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "GUIDE");
            metadata.put("guide_id", guideId);
            metadata.put("title", title);
            // TODO: heading anchor 추출하여 metadata.put("anchor", ...)
            EmbeddingChunk chunk = new EmbeddingChunk(RefType.GUIDE, guideId, text, metadata);
            chunkRepository.save(chunk);
            // TODO: 임베딩 벡터 계산/저장
        }
        log.debug("[Embedding] guide {} → {} chunks", guideId, chunks.size());
    }

    /**
     * 게시된 가이드(및 필요 시 원본) 대상 하이브리드 검색.
     * TODO: 벡터 유사도(pgvector cosine) + 키워드(BM25) 하이브리드 구현.
     */
    @Transactional(readOnly = true)
    public List<RetrievedChunk> search(Long projectId, String query, int topK) {
        log.debug("[Embedding] search projectId={} topK={} query={}", projectId, topK, query);
        // TODO: EmbeddingModel.embed(query) → SELECT ... ORDER BY embedding <=> :vec LIMIT topK
        return List.of();
    }
}
