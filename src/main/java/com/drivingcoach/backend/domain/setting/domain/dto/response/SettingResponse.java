package com.drivingcoach.backend.domain.setting.domain.dto.response;

import com.drivingcoach.backend.domain.setting.domain.entity.Setting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * ✅ SettingResponse
 *
 * 용도
 *  - 환경설정(Setting) 엔티티를 외부로 응답할 때 사용하는 DTO입니다.
 *  - 엔티티를 직접 노출하지 않고 필요한 필드만 안전하게 전달합니다.
 *
 * 포함 필드
 *  - vibrationEnabled : 진동 알림 사용 여부
 *  - feedbackVoice    : 음성 피드백 모드 (예: off / male / female / tts_ko)
 *
 * 팁
 *  - from(Setting) 정적 팩토리로 변환을 표준화합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingResponse {

    @Schema(description = "진동 알림 사용 여부", example = "true")
    private boolean vibrationEnabled;

    @Schema(description = "음성 피드백 모드", example = "tts_ko")
    private String feedbackVoice;

    /** Entity → DTO 변환 */
    public static SettingResponse from(Setting s) {
        return SettingResponse.builder()
                .vibrationEnabled(s.isVibrationEnabled())
                .feedbackVoice(s.getFeedbackVoice())
                .build();
    }
}
