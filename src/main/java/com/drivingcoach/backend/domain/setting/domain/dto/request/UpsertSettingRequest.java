package com.drivingcoach.backend.domain.setting.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * ✅ UpsertSettingRequest
 *
 * 용도
 *  - 설정 일괄 업서트(존재하지 않으면 생성, 있으면 갱신) 요청 DTO입니다.
 *  - 엔드포인트: PUT /api/setting
 *
 * 필드 설명
 *  - vibrationEnabled : (선택) 진동 사용 여부. null 이면 변경하지 않습니다.
 *  - feedbackVoice    : (선택) 음성 피드백 모드. null/blank 이면 변경하지 않습니다.
 *                       예) "off", "male", "female", "tts_ko"
 *
 * 검증/정책
 *  - 모든 필드는 Optional 로 간주합니다(부분 업데이트 패턴).
 *  - feedbackVoice 는 DB 컬럼 길이(30자)와 동일한 제한을 둡니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertSettingRequest {

    @Schema(description = "진동 사용 여부(옵션)", example = "true")
    private Boolean vibrationEnabled;

    @Schema(description = "음성 피드백 모드(옵션)", example = "tts_ko")
    @Size(max = 30, message = "feedbackVoice 는 최대 30자입니다.")
    private String feedbackVoice;
}
