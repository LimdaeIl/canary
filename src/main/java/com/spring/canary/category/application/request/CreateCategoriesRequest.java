package com.spring.canary.category.application.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record CreateCategoriesRequest(
        @NotEmpty(message = "카테고리: 카테고리 이름은 한 개 이상이어야 합니다.")
        @Size(max = 5, message = "카테고리: 카테고리의 최대 깊이는 5 입니다.")
        @UniqueElements(message = "카테고리: 입력한 카테고리 이름에서 중복된 카테고리명이 존재합니다.")
        List<
                @NotNull(message = "카테고리: 카테고리 이름은 필수 입니다.")
                @Size(max = 24, message = "카테고리: 카테고리 이름은 최대 24글자 입니다.")
                        String> names
) {

}
