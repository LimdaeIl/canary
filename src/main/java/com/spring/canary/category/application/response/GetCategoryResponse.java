package com.spring.canary.category.application.response;

import com.spring.canary.category.domain.Category;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record GetCategoryResponse(
        Long id,
        String name,
        Long parentId
) {

    public static GetCategoryResponse of(Category created, Long parentId) {
        return GetCategoryResponse.builder()
                .id(created.getCategoryId())
                .name(created.getName())
                .parentId(parentId)
                .build();
    }
}