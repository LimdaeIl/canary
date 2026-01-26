package com.book.canary.category.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryNameRequest(
        @NotBlank(message = "카테고리: 카테고리명은 필수 입니다.")
        String name
) {

}
