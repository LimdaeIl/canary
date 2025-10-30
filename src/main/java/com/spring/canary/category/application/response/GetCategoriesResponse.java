package com.spring.canary.category.application.response;

import com.spring.canary.category.domain.Category;
import java.util.List;

public record GetCategoriesResponse(

) {

    public static GetCategoryResponse from(List<Category> categories) {
        return null;
    }
}
