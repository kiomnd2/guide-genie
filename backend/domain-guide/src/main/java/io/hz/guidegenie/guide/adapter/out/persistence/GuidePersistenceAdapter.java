package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.application.port.out.GuideRepositoryPort;
import io.hz.guidegenie.guide.domain.Guide;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuidePersistenceAdapter implements GuideRepositoryPort {

    private final GuideJpaRepository jpa;

    @Override
    public Guide save(Guide guide) {
        return jpa.save(guide);
    }

    @Override
    public List<Guide> findByProjectId(Long projectId) {
        return jpa.findByProjectId(projectId);
    }

    @Override
    public Optional<Guide> findById(Long id) {
        return jpa.findById(id);
    }
}
