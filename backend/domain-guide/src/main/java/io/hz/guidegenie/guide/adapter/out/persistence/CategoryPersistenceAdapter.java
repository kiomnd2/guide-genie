package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.application.port.out.CategoryRepositoryPort;
import io.hz.guidegenie.guide.domain.Category;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository jpa;

    @Override
    public Category save(Category category) {
        return jpa.save(category);
    }

    @Override
    public List<Category> findByProjectId(Long projectId) {
        return jpa.findByProjectIdOrderBySortOrderAscIdAsc(projectId);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
