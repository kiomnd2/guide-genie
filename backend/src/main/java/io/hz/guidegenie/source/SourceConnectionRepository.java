package io.hz.guidegenie.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceConnectionRepository extends JpaRepository<SourceConnection, Long> {

    List<SourceConnection> findByProjectId(Long projectId);
}
