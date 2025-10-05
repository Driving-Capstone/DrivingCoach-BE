package com.drivingcoach.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 정의
 * - 각 코드별 기본 HTTP 상태와 메시지를 포함
 */
@Getter
public enum ErrorCode {

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 사용자/인증
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CANNOT_BE_SAME(HttpStatus.BAD_REQUEST, "이전 비밀번호와 동일할 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 엑세스 토큰입니다."),
    TOKEN_ALREADY_LOGOUT(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INCORRECT_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),


    // 업로드/외부 연동
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 업로드에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.httpStatus = status;
        this.message = message;
    }


}
