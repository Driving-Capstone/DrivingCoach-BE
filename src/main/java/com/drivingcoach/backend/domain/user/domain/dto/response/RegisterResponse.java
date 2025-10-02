package com.drivingcoach.backend.domain.user.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 회원가입 결과 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    @Schema(description = "가입된 로그인 ID", example = "drive_king01")
    private String loginId;
}
