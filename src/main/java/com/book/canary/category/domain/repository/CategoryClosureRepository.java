package com.book.canary.category.domain.repository;

import com.book.canary.category.domain.entity.Category;
import com.book.canary.category.domain.entity.CategoryClosure;
import com.book.canary.category.domain.entity.CategoryClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryClosureRepository extends
        JpaRepository<CategoryClosure, CategoryClosureId> {

    @Query(value = """
            select COALESCE(max(cc.depth))
            from category_closures as cc
            WHERE cc.descendant_id = :parentId
            """, nativeQuery = true)
    int findTopDepthByParentId(@Param("parentId") Long parentId);


    @Query(value = """
            select cc.*
            from category_closures as cc
            where cc.descendant_id = :parentId
            """, nativeQuery = true)
    List<CategoryClosure> findByAllDescendant(@Param("parentId") Long parentId);

}
