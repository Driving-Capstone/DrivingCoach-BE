package com.drivingcoach.backend.domain.driving.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ EndDrivingRequest
 *
 * 용도
 *  - "주행 종료" API 요청 바디 (POST /api/driving/end)
 *
 * 필드 설명
 *  - recordId            : (필수) 종료할 주행 기록 ID. 반드시 "내 소유" 기록이어야 하며, 서비스에서 소유권 검증을 수행합니다.
 *  - endTime             : (선택) 주행 종료 시각. null 이면 서버에서 현재 시각(LocalDateTime.now())으로 대체합니다.
 *  - finalScore          : (선택) 분석 완료된 최종 점수. null 이면 점수 업데이트를 생략합니다.
 *                          - 0.0 ~ 100.0 범위를 권장하며, 서비스에서 범위를 보정(clamp)합니다.
 *  - finalVideoKeyOrUrl  : (선택) 최종 확정된 영상/zip의 S3 키 또는 CDN/퍼블릭 URL.
 *                          - 웹소켓/스트리밍 업로드 후 마지막에 대표 키를 저장하는 패턴을 지원합니다.
 *
 * 검증 정책
 *  - recordId 는 필수
 *  - finalVideoKeyOrUrl 은 최대 512자
 *  - finalScore 는 음수 불가(하한만 검증). 상한은 서비스에서 보정 처리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndDrivingRequest {

    @Schema(description = "종료할 주행 기록 ID(필수)", example = "123")
    @NotNull(message = "recordId 는 필수입니다.")
    private Long recordId;

    @Schema(description = "주행 종료 시각(옵션). 미전달 시 서버 now 적용", example = "2025-09-28T11:22:33")
    private LocalDateTime endTime;

    @Schema(description = "최종 점수(옵션, 0~100 권장)", example = "87.5")
    @Min(value = 0, message = "finalScore 는 0 이상이어야 합니다.")
    private Float finalScore;

    @Schema(description = "최종 영상/zip S3 키 또는 URL(옵션)", example = "driving/abc-uuid/final.zip")
    @Size(max = 512, message = "finalVideoKeyOrUrl 은 512자를 넘을 수 없습니다.")
    private String finalVideoKeyOrUrl;
}
