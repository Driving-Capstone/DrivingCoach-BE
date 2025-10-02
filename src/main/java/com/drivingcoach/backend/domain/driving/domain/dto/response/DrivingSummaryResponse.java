package com.drivingcoach.backend.domain.driving.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * ✅ DrivingSummaryResponse
 *
 * 용도
 *  - 기간 요약 통계를 반환할 때 사용하는 단순 응답 DTO입니다.
 *  - 엔드포인트: GET /api/driving/stats/summary?from=...&to=...
 *
 * 포함 필드
 *  - totalSeconds : 해당 기간의 총 주행 시간(초)
 *  - averageScore : 해당 기간의 평균 점수 (모든 기록의 score 평균; 점수 없는 기록은 제외)
 *
 * 주의
 *  - averageScore 는 null 일 수 있습니다(해당 기간에 점수가 하나도 없을 때).
 *  - totalSeconds 는 항상 0 이상입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrivingSummaryResponse {

    @Schema(description = "총 주행 시간(초)", example = "7380")
    private int totalSeconds;

    @Schema(description = "평균 점수(null 가능)", example = "83.4")
    private Double averageScore;
}
