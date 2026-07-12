package io.hz.guidegenie.rag.application.port.out;

import io.hz.guidegenie.rag.domain.EmbeddingChunk;
import io.hz.guidegenie.rag.domain.RefType;

/** 아웃바운드 포트 — 임베딩 청크 영속화. */
public interface EmbeddingChunkRepositoryPort {
    EmbeddingChunk save(EmbeddingChunk chunk);
    void deleteByRef(RefType refType, Long refId);
}
