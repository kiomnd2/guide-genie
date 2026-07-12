package io.hz.guidegenie.guide.application.port.out;

import io.hz.guidegenie.guide.domain.GuideRevision;
import java.util.List;
import java.util.Optional;

public interface GuideRevisionRepositoryPort {
    GuideRevision save(GuideRevision revision);
    List<GuideRevision> findByGuideIdOrderByVersionDesc(Long guideId);
    Optional<GuideRevision> findLatest(Long guideId);
}
