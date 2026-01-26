package com.book.canary.category.domain.repository;

import com.book.canary.category.domain.entity.Category;
import com.book.canary.category.domain.entity.CategoryClosure;
import com.book.canary.category.domain.entity.CategoryClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 리프 여부: 자식(직계)이 하나라도 있으면 리프가 아님
    @Query(value = """
        SELECT CASE WHEN EXISTS (
          SELECT 1
          FROM category_closures
          WHERE ancestor_id = :id
            AND depth = 1
        ) THEN 0 ELSE 1 END
        """, nativeQuery = true)
    int isLeaf(@Param("id") Long id);

    // 대상 노드가 포함된 모든 경로 삭제(자기 자신 경로 포함)
    @Modifying
    @Query(value = """
        DELETE FROM category_closures
        WHERE ancestor_id = :id OR descendant_id = :id
        """, nativeQuery = true)
    int deleteAllPathsOfNode(@Param("id") Long id);
}
