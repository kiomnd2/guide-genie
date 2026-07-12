package io.hz.guidegenie.source.adapter.out.persistence;

import io.hz.guidegenie.source.application.port.out.SourceDocumentRepositoryPort;
import io.hz.guidegenie.source.domain.SourceDocument;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SourceDocumentPersistenceAdapter implements SourceDocumentRepositoryPort {

    private final SourceDocumentJpaRepository jpa;

    @Override
    public SourceDocument save(SourceDocument document) {
        return jpa.save(document);
    }

    @Override
    public Optional<SourceDocument> findByConnectionIdAndExternalId(Long connectionId, String externalId) {
        return jpa.findByConnectionIdAndExternalId(connectionId, externalId);
    }
}
