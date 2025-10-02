package com.drivingcoach.backend.domain.setting.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ✅ UpdateVibrationRequest
 *
 * 용도
 *  - 진동 설정 on/off 를 부분 업데이트(PATCH)할 때 사용하는 요청 DTO입니다.
 *  - 엔드포인트: PATCH /api/setting/vibration
 *
 * 필드
 *  - enabled : true 이면 진동 사용, false 이면 진동 미사용
 *
 * 검증
 *  - enabled 는 null 이면 안 됩니다(@NotNull).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVibrationRequest {

    @Schema(description = "진동 사용 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "enabled 값은 필수입니다.")
    private Boolean enabled;

    /** 편의: primitive 로 꺼낼 때 NPE 방지용 */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
