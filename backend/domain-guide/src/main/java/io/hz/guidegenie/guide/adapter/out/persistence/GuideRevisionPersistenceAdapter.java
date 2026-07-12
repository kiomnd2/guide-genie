package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.application.port.out.GuideRevisionRepositoryPort;
import io.hz.guidegenie.guide.domain.GuideRevision;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuideRevisionPersistenceAdapter implements GuideRevisionRepositoryPort {

    private final GuideRevisionJpaRepository jpa;

    @Override
    public GuideRevision save(GuideRevision revision) {
        return jpa.save(revision);
    }

    @Override
    public List<GuideRevision> findByGuideIdOrderByVersionDesc(Long guideId) {
        return jpa.findByGuideIdOrderByVersionDesc(guideId);
    }

    @Override
    public Optional<GuideRevision> findLatest(Long guideId) {
        return jpa.findFirstByGuideIdOrderByVersionDesc(guideId);
    }
}
