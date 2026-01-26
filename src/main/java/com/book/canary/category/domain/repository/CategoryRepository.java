package com.book.canary.category.domain.repository;

import com.book.canary.category.domain.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    /** 루트: depth=1 부모-자식 관계가 존재하지 않는 카테고리 */
    @Query(value = """
        SELECT c.*
        FROM categories c
        WHERE NOT EXISTS (
          SELECT 1
          FROM category_closures cc
          WHERE cc.descendant_id = c.category_id
            AND cc.depth = 1
        )
        ORDER BY c.name
        """, nativeQuery = true)
    List<Category> findAllRoot();

    /** 모든 카테고리 + 즉시부모(parentId) 한 번에 */
    @Query(value = """
            SELECT
              c.category_id  AS id,
              c.name         AS name,
              cc.ancestor_id AS parentId
            FROM categories c
            LEFT JOIN category_closures cc
              ON cc.descendant_id = c.category_id
             AND cc.depth = 1
            """, nativeQuery = true)
    List<Row> findAllWithParent();

    /** 특정 id를 루트로 하는 서브트리 + 즉시부모(parentId) */
    @Query(value = """
        SELECT
          d.category_id AS id,
          d.name        AS name,
          CASE WHEN d.category_id = :rootId THEN NULL ELSE p.ancestor_id END AS parentId
        FROM category_closures x         -- :rootId의 (자기 포함) 모든 후손
        JOIN categories d ON d.category_id = x.descendant_id
        LEFT JOIN category_closures p
          ON p.descendant_id = d.category_id
         AND p.depth = 1
        WHERE x.ancestor_id = :rootId
        """, nativeQuery = true)
    List<Row> findSubtreeWithParent(@Param("rootId") Long rootId);

    /** 네이티브 인터페이스 프로젝션은 getXxx() 형태가 필수 */
    interface Row {
        Long getId();
        String getName();
        Long getParentId();
    }
}

