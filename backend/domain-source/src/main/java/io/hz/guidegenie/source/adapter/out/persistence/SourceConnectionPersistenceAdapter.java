package io.hz.guidegenie.source.adapter.out.persistence;

import io.hz.guidegenie.source.application.port.out.SourceConnectionRepositoryPort;
import io.hz.guidegenie.source.domain.SourceConnection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SourceConnectionPersistenceAdapter implements SourceConnectionRepositoryPort {

    private final SourceConnectionJpaRepository jpa;

    @Override
    public SourceConnection save(SourceConnection connection) {
        return jpa.save(connection);
    }

    @Override
    public List<SourceConnection> findByProjectId(Long projectId) {
        return jpa.findByProjectId(projectId);
    }

    @Override
    public List<SourceConnection> findAll() {
        return jpa.findAll();
    }

    @Override
    public Optional<SourceConnection> findById(Long id) {
        return jpa.findById(id);
    }
}
