package io.hz.guidegenie.guide.application.service;

import io.hz.guidegenie.common.NotFoundException;
import io.hz.guidegenie.guide.application.port.out.CategoryRepositoryPort;
import io.hz.guidegenie.guide.domain.Category;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepositoryPort repository;

    @Transactional
    public Category create(Long projectId, String name, Long parentId) {
        if (parentId != null) {
            Category parent = get(parentId);
            if (!parent.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("parent category belongs to another project");
            }
            if (!parent.isMajor()) {
                throw new IllegalArgumentException("중분류 아래에는 분류를 만들 수 없습니다(2단계까지).");
            }
        }
        return repository.save(new Category(projectId, parentId, name));
    }

    @Transactional(readOnly = true)
    public List<Category> findByProject(Long projectId) {
        return repository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    @Transactional
    public Category rename(Long id, String name) {
        Category category = get(id);
        category.rename(name);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** 카테고리가 해당 프로젝트 소속인지 검증(가이드 이동 시 사용). null은 미분류로 허용. */
    @Transactional(readOnly = true)
    public void assertInProject(Long categoryId, Long projectId) {
        if (categoryId == null) {
            return;
        }
        Category c = get(categoryId);
        if (!c.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("category belongs to another project");
        }
    }
}
