package com.spring.canary.category.infrastructure.mybatis;

import com.spring.canary.category.domain.Category;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {
    int insertCategories(@Param("list") List<Category> list);
}
