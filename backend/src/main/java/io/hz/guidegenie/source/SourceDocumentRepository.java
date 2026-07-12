package io.hz.guidegenie.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {

    List<SourceDocument> findByConnectionId(Long connectionId);

    Optional<SourceDocument> findByConnectionIdAndExternalId(Long connectionId, String externalId);
}
