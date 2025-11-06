package com.book.canary.category.domain.repository;

import com.book.canary.category.domain.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {


    boolean existsByName(String name);

    @Query(value = """
            select c.*
            from categories as c
            where not exists(
                select 1
                from category_closures as cc
                where cc.descendant_id = c.category_id
                and depth = 1
            )
            """, nativeQuery = true)
    List<Category> findAllRoot();


    @Query(value = """
            select c.category_id as id, c.name as name, cc.ancestor_id as parentId
            from categories as c
            left join category_closures as cc
                on cc.descendant_id = c.category_id
                and cc.depth = 1
            """, nativeQuery = true)
    List<Row> findAllByRelationShip();

    interface Row {

        Long getId();

        String getName();

        Long getParentId();
    }

}
