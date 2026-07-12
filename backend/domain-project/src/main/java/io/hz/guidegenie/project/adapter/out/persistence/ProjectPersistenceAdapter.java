package io.hz.guidegenie.project.adapter.out.persistence;

import io.hz.guidegenie.project.application.port.out.ProjectRepositoryPort;
import io.hz.guidegenie.project.domain.Project;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — ProjectRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepositoryPort {

    private final ProjectJpaRepository jpa;

    @Override
    public Project save(Project project) {
        return jpa.save(project);
    }

    @Override
    public List<Project> findAll() {
        return jpa.findAll();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
