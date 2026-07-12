package io.hz.guidegenie.project;

import io.hz.guidegenie.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public Project create(String name, String description, String owner) {
        return projectRepository.save(new Project(name, description, owner));
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Project get(Long id) {
        return projectRepository.findById(id)
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
        projectRepository.deleteById(id);
    }
}
