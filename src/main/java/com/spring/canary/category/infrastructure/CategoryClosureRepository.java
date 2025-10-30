package com.spring.canary.category.infrastructure;

import com.spring.canary.category.domain.CategoryClosure;
import com.spring.canary.category.domain.CategoryClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryClosureRepository extends JpaRepository<CategoryClosure, CategoryClosureId> {


    boolean existsByCategoryClosureId_AncestorIdAndLevel(Long ancestorId, int level);

    List<CategoryClosure> findAllByCategoryClosureId_DescendantId(Long descendantId);
}
