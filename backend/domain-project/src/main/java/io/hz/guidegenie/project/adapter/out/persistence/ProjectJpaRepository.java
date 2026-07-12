package io.hz.guidegenie.project.adapter.out.persistence;

import io.hz.guidegenie.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA — 영속 어댑터 내부 구현(외부엔 포트로만 노출). */
public interface ProjectJpaRepository extends JpaRepository<Project, Long> {
}
