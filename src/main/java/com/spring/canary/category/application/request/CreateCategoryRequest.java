package com.spring.canary.category.application.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotNull(message = "카테고리: 카테고리 이름은 필수 입니다.")
        @Size(max = 24, message = "카테고리: 카테고리 이름은 최대 24글자 입니다.")
        String name,

        @Positive(message = "카테고리: 카테고리 ID가 올바르지 않습니다.")
        Long parentId
) {

}
