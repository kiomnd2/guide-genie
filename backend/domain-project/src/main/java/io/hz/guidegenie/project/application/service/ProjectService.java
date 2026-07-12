package io.hz.guidegenie.project.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.project.application.port.out.ProjectRepositoryPort;
import io.hz.guidegenie.project.domain.Project;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트 유스케이스. 아웃바운드 포트에만 의존. */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepositoryPort repository;

    @Transactional
    public Project create(String name, String description, String owner) {
        return repository.save(new Project(name, description, owner));
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Project get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    @Transactional
    public Project update(Long id, String name, String description) {
        Project project = get(id);
        project.update(name, description);
        return project;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
