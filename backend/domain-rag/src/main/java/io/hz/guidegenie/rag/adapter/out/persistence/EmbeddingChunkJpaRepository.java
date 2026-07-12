package io.hz.guidegenie.rag.adapter.out.persistence;

import io.hz.guidegenie.rag.domain.EmbeddingChunk;
import io.hz.guidegenie.rag.domain.RefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmbeddingChunkJpaRepository extends JpaRepository<EmbeddingChunk, Long> {

    @Modifying
    @Query("delete from EmbeddingChunk c where c.refType = :refType and c.refId = :refId")
    void deleteByRefTypeAndRefId(@Param("refType") RefType refType, @Param("refId") Long refId);
}
