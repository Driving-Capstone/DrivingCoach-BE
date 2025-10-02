package com.drivingcoach.backend.domain.user.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 비밀번호 변경 요청 DTO
 * - currentPassword: 현재 비밀번호 (검증 용도)
 * - newPassword: 새 비밀번호 (서버에서 정책 검증)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @Schema(description = "현재 비밀번호", example = "oldPassword123!")
    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    @Schema(description = "새 비밀번호 (8~64자, 영문/숫자/특수문자 조합 권장)", example = "NewPassword!234")
    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String newPassword;
}
