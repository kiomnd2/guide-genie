package io.hz.guidegenie.guide.application.port.out;

import io.hz.guidegenie.guide.domain.GuideTemplate;
import java.util.List;
import java.util.Optional;

public interface GuideTemplateRepositoryPort {
    GuideTemplate save(GuideTemplate template);
    List<GuideTemplate> findByProjectId(Long projectId);
    Optional<GuideTemplate> findById(Long id);
    void deleteById(Long id);
}
