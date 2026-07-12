package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.application.port.out.GuideTemplateRepositoryPort;
import io.hz.guidegenie.guide.domain.GuideTemplate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuideTemplatePersistenceAdapter implements GuideTemplateRepositoryPort {

    private final GuideTemplateJpaRepository jpa;

    @Override
    public GuideTemplate save(GuideTemplate template) {
        return jpa.save(template);
    }

    @Override
    public List<GuideTemplate> findByProjectId(Long projectId) {
        return jpa.findByProjectIdOrderByIdAsc(projectId);
    }

    @Override
    public Optional<GuideTemplate> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
