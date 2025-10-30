package com.spring.canary.category.domain.exception;

import com.spring.canary.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum CategoryErrorCode implements ErrorCode {
    DUPLICATE_NAME(HttpStatus.CONFLICT, "카테고리: 이미 존재하는 이름입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리: 카테고리가 존재하지 않습니다."),
    NOT_LEAF_PARENT(HttpStatus.BAD_REQUEST, "카테고리: 마지막 카테고리에만 추가할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
