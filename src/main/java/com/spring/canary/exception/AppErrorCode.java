package com.spring.canary.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AppErrorCode implements ErrorCode {

    // 시스템
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "공통: 서버 내부 오류가 발생했습니다."),

    // Snowflake
    SNOWFLAKE_NODE_ID_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR,
            "식별자: Snowflake node-id가 설정되지 않았습니다. auto-detect-node-id=false인 경우 필수입니다.");


    private final HttpStatus status;
    private final String message;
}
