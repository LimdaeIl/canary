package com.book.canary.category.application.dto.response;

import java.util.List;

public record CreateCategoryResponse(
        Long id,
        String name,
        Long parentId,
        int depth,
        List<BreadcrumbNode> breadcrumb   // 조상 → 자기 자신
) {

    public record BreadcrumbNode(Long id, String name) {

    }
}
