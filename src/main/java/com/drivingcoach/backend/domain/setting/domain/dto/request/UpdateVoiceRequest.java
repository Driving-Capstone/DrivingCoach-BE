package com.drivingcoach.backend.domain.setting.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * ✅ UpdateVoiceRequest
 *
 * 용도
 *  - 음성 피드백 모드만 부분 업데이트(PATCH)할 때 사용하는 요청 DTO입니다.
 *  - 엔드포인트: PATCH /api/setting/voice
 *
 * 필드
 *  - voiceMode : 음성 피드백 모드 문자열
 *               예) "off", "male", "female", "tts_ko"
 *               초기에는 문자열로 운영하고, 모드가 고정되면 Enum 으로 승격하는 것을 권장합니다.
 *
 * 검증
 *  - NotBlank : 비어 있으면 안 됩니다.
 *  - Size(max=30) : DB 컬럼 길이와 맞춥니다(Setting.feedbackVoice length=30).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVoiceRequest {

    @Schema(description = "음성 피드백 모드", example = "tts_ko", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "voiceMode 는 비어 있을 수 없습니다.")
    @Size(max = 30, message = "voiceMode 는 최대 30자입니다.")
    private String voiceMode;
}
