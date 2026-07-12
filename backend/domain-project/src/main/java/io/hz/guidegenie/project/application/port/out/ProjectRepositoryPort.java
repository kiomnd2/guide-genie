package io.hz.guidegenie.project.application.port.out;

import io.hz.guidegenie.project.domain.Project;
import java.util.List;
import java.util.Optional;

/** 아웃바운드 포트 — 프로젝트 영속화. 구현: adapter.out.persistence.ProjectPersistenceAdapter. */
public interface ProjectRepositoryPort {
    Project save(Project project);
    List<Project> findAll();
    Optional<Project> findById(Long id);
    void deleteById(Long id);
}
