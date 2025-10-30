package com.spring.canary.category.domain.exception;

import com.spring.canary.exception.AppException;

public class CategoryException extends AppException {

    public CategoryException(CategoryErrorCode categoryErrorCode) {
        super(categoryErrorCode);
    }
}
