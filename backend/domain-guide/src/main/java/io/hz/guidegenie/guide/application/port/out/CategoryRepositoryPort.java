package io.hz.guidegenie.guide.application.port.out;

import io.hz.guidegenie.guide.domain.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    Category save(Category category);
    List<Category> findByProjectId(Long projectId);
    Optional<Category> findById(Long id);
    void deleteById(Long id);
}
