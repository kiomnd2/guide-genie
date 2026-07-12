package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.domain.GuideRevision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideRevisionJpaRepository extends JpaRepository<GuideRevision, Long> {
    List<GuideRevision> findByGuideIdOrderByVersionDesc(Long guideId);
    Optional<GuideRevision> findFirstByGuideIdOrderByVersionDesc(Long guideId);
}
