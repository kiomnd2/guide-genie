package io.hz.guidegenie.source.adapter.out.persistence;

import io.hz.guidegenie.source.domain.SourceConnection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceConnectionJpaRepository extends JpaRepository<SourceConnection, Long> {
    List<SourceConnection> findByProjectId(Long projectId);
}
