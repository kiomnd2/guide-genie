package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.domain.GuideTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideTemplateJpaRepository extends JpaRepository<GuideTemplate, Long> {
    List<GuideTemplate> findByProjectIdOrderByIdAsc(Long projectId);
}
