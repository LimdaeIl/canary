package com.book.canary.category.application.dto.response;

import com.book.canary.category.domain.entity.Category;

public record UpdateCategoryNameResponse(
        Long id,
        String name
) {

    public static UpdateCategoryNameResponse from(Category category) {
        return new UpdateCategoryNameResponse(category.getId(), category.getName());
    }
}
