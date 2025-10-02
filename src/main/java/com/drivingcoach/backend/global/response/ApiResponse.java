package com.drivingcoach.backend.global.response;

import com.drivingcoach.backend.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 공통 응답 포맷
 * - 성공/실패에 상관없이 동일한 포맷 유지
 * - 성공 시: success=true, code=200, message="OK", data=...
 * - 실패 시: success=false, code=에러코드 HTTP 상태값, message=에러 메시지, data=null
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    @Builder.Default
    private boolean success = true;

    @Schema(description = "상태 코드(HTTP 유사)", example = "200")
    @Builder.Default
    private int code = 200;

    @Schema(description = "메시지", example = "OK")
    @Builder.Default
    private String message = "OK";

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "응답 시각(ISO-8601)")
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /* ---------- 성공 응답 ---------- */

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message("OK")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message("OK")
                .build();
    }

    /* ---------- 실패 응답 ---------- */

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
