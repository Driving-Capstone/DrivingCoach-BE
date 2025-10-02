package com.drivingcoach.backend.global.exception;

import lombok.Getter;

/**
 * 비즈니스 예외의 공통 래퍼
 * - 서비스/도메인 층에서 throw 하고, 전역 예외 처리기에서 잡아 응답 변환
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public int getHttpStatusValue() {
        return errorCode.getHttpStatus().value();
    }
}
