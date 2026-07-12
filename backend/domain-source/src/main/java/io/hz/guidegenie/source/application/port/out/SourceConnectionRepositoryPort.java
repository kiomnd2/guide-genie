package io.hz.guidegenie.source.application.port.out;

import io.hz.guidegenie.source.domain.SourceConnection;
import java.util.List;
import java.util.Optional;

public interface SourceConnectionRepositoryPort {
    SourceConnection save(SourceConnection connection);
    List<SourceConnection> findByProjectId(Long projectId);
    List<SourceConnection> findAll();
    Optional<SourceConnection> findById(Long id);
}
