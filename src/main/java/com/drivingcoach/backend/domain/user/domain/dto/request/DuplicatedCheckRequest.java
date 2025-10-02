package com.drivingcoach.backend.domain.user.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 로그인ID 중복 체크 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicatedCheckRequest {

    @Schema(description = "중복 확인할 로그인 ID", example = "drive_king01")
    @NotBlank(message = "로그인 ID를 입력해 주세요.")
    private String loginId;
}
