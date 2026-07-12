package io.hz.guidegenie.guide.adapter.out.persistence;

import io.hz.guidegenie.guide.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
    List<Category> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
}
