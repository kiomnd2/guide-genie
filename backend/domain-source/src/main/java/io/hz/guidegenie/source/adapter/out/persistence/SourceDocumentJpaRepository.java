package io.hz.guidegenie.source.adapter.out.persistence;

import io.hz.guidegenie.source.domain.SourceDocument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentJpaRepository extends JpaRepository<SourceDocument, Long> {
    Optional<SourceDocument> findByConnectionIdAndExternalId(Long connectionId, String externalId);
}
