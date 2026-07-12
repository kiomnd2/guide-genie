package io.hz.guidegenie.source.application.port.out;

import io.hz.guidegenie.source.domain.SourceDocument;
import java.util.Optional;

public interface SourceDocumentRepositoryPort {
    SourceDocument save(SourceDocument document);
    Optional<SourceDocument> findByConnectionIdAndExternalId(Long connectionId, String externalId);
}
