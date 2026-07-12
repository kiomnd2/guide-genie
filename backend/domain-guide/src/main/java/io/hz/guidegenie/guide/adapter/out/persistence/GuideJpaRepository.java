package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.domain.Guide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideJpaRepository extends JpaRepository<Guide, Long> {
    List<Guide> findByProjectId(Long projectId);
}
