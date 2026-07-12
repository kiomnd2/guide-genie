package io.hz.guidegenie.guide;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideRevisionRepository extends JpaRepository<GuideRevision, Long> {

    List<GuideRevision> findByGuideIdOrderByVersionDesc(Long guideId);

    Optional<GuideRevision> findFirstByGuideIdOrderByVersionDesc(Long guideId);
}
