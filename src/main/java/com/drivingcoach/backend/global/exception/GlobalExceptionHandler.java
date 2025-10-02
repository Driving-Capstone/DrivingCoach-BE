package com.drivingcoach.backend.global.exception;

import com.drivingcoach.backend.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* -------- 비즈니스 커스텀 예외 -------- */
    @ExceptionHandler(CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException e) {
        log.warn("[BUSINESS] {} - {}", e.getErrorCode(), e.getMessage());
        return ApiResponse.error(e.getHttpStatusValue(), e.getMessage());
    }

    /* -------- 인증/인가 관련 -------- */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("[SECURITY] Access denied: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ApiResponse<Void> handleUsernameNotFound(UsernameNotFoundException e) {
        log.warn("[SECURITY] Username not found: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.UNAUTHORIZED);
    }

    /* -------- 요청 바인딩/검증 -------- */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("[VALIDATION] {}", msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST.getHttpStatus().value(), msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("[BIND] {}", msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST.getHttpStatus().value(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("[CONSTRAINT] {}", msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST.getHttpStatus().value(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[NOT_READABLE] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_REQUEST);
    }

    /* -------- 최후 방어 -------- */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("[UNHANDLED] {}", e.getMessage(), e);
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
