package io.hz.guidegenie.guide;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByProjectId(Long projectId);
}
