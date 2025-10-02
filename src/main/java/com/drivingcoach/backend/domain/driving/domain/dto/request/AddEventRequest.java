package com.drivingcoach.backend.domain.driving.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ AddEventRequest
 *
 * 용도
 *  - 특정 주행 기록(DrivingRecord)에 "이벤트"를 추가할 때 사용되는 요청 바디입니다.
 *  - 엔드포인트: POST /api/driving/{recordId}/events
 *
 * 필드 설명
 *  - type       : (필수) 이벤트 유형. 예) "급가속", "rapid_accel", "lane_departure" 등
 *                 초기에는 문자열로 운영하고, 유형이 안정화되면 Enum으로 승격하는 것을 권장합니다.
 *  - eventTime  : (선택) 이벤트 발생 시각. 미전달(null)이면 서버에서 now 를 사용합니다.
 *                 클라이언트-서버 시간 싱크가 애매하면 null을 보내 서버 시간을 기준으로 관리하는 편이 안전합니다.
 *  - severity   : (선택) 심각도. 예) "low" / "medium" / "high"
 *                 미전달 시 서비스에서 "low" 등 기본값을 부여합니다.
 *  - note       : (선택) 디버깅/설명용 메모. UI 표시가 필요 없으면 저장하지 않아도 됩니다.
 *
 * 검증 정책
 *  - type 은 NotBlank
 *  - severity, note 는 길이 제한만 두고 필수 아님
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddEventRequest {

    @Schema(description = "이벤트 유형(필수)", example = "lane_departure")
    @NotBlank(message = "이벤트 유형(type)은 비어 있을 수 없습니다.")
    @Size(max = 50, message = "이벤트 유형은 최대 50자입니다.")
    private String type;

    @Schema(description = "이벤트 발생 시각(옵션). 미전달 시 서버 now 적용", example = "2025-09-28T10:12:30")
    private LocalDateTime eventTime;

    @Schema(description = "심각도(옵션, 예: low/medium/high)", example = "high")
    @Size(max = 20, message = "심각도(severity)는 최대 20자입니다.")
    private String severity;

    @Schema(description = "추가 메모(옵션)", example = "차로 이탈 감지됨(우측).")
    @Size(max = 300, message = "메모(note)는 최대 300자입니다.")
    private String note;
}
