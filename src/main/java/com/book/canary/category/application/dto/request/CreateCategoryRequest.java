package com.book.canary.category.application.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "카테고리: 이름은 필수 입니다.")
        @Size(max = 50, message = "최대 50 글자 이내이어야 합니다.")
        String name,

        Long parentId
) {

}
