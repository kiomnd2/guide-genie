package io.hz.guidegenie.embedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunk, Long> {

    @Modifying
    @Query("delete from EmbeddingChunk c where c.refType = :refType and c.refId = :refId")
    void deleteByRef(@Param("refType") RefType refType, @Param("refId") Long refId);
}
