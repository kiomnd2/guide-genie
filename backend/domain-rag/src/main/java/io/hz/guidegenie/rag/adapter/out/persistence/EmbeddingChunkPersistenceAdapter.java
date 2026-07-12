package io.hz.guidegenie.rag.adapter.out.persistence;

import io.hz.guidegenie.rag.application.port.out.EmbeddingChunkRepositoryPort;
import io.hz.guidegenie.rag.domain.EmbeddingChunk;
import io.hz.guidegenie.rag.domain.RefType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — EmbeddingChunkRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class EmbeddingChunkPersistenceAdapter implements EmbeddingChunkRepositoryPort {

    private final EmbeddingChunkJpaRepository jpa;

    @Override
    public EmbeddingChunk save(EmbeddingChunk chunk) {
        return jpa.save(chunk);
    }

    @Override
    public void deleteByRef(RefType refType, Long refId) {
        jpa.deleteByRefTypeAndRefId(refType, refId);
    }
}
