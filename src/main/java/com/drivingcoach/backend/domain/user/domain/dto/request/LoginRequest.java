package com.drivingcoach.backend.domain.user.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 로그인 요청 DTO
 * - loginId / password 기반 인증
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @Schema(description = "로그인 ID", example = "drive_king01")
    @NotBlank(message = "로그인 ID를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "로그인 ID는 4~20자여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호", example = "Password!234")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;
}
