package io.hz.guidegenie.guide.application.port.out;

import io.hz.guidegenie.guide.domain.Guide;
import java.util.List;
import java.util.Optional;

public interface GuideRepositoryPort {
    Guide save(Guide guide);
    List<Guide> findByProjectId(Long projectId);
    Optional<Guide> findById(Long id);
    void deleteById(Long id);
}
