package com.drivingcoach.backend.domain.home.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ WeeklyStatusResponse
 *
 * 용도
 *  - 홈 화면 "주간 요약" API 응답 모델.
 *  - 총 주행 시간, 평균 점수, 일자별 합계(차트용), 최근 주행 요약을 포함합니다.
 *
 * 기간 규칙
 *  - [from, to) 반개구간을 사용합니다. (from 이상, to 미만)
 *  - 컨트롤러/서비스에서 타임존 경계(자정)를 맞춘 뒤 값을 채워 넣습니다.
 *
 * 구성
 *  - from / to           : 조회 기간 경계
 *  - totalSeconds        : 기간 내 총 주행 시간(초)
 *  - averageScore        : 기간 내 평균 점수 (null 가능)
 *  - dailySeconds        : 일자별 총 주행 시간 버킷(차트/스파크라인 등 UI에 사용)
 *  - lastDriving         : 최근 주행 1건 요약(선택 정보)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyStatusResponse {

    @Schema(description = "기간 시작 (포함)", example = "2025-09-22T00:00:00")
    private LocalDateTime from;

    @Schema(description = "기간 끝 (미포함)", example = "2025-09-29T00:00:00")
    private LocalDateTime to;

    @Schema(description = "총 주행 시간(초)", example = "7380")
    private int totalSeconds;

    @Schema(description = "평균 점수(null 가능)", example = "83.4")
    private Double averageScore;

    @Schema(description = "일자별 총 주행(초) 버킷")
    private List<DayBucket> dailySeconds;

    @Schema(description = "가장 최근 주행 요약(선택)")
    private LastDriving lastDriving;

    /* ---------- 하위 타입 ---------- */

    /**
     * 📅 DayBucket
     *  - 특정 날짜(LocalDate)와 그 날짜의 총 주행 시간(초)
     *  - Java 16+ record 로 간결하게 정의 (불변)
     *  - 사용 예: new DayBucket(LocalDate.of(2025,9,28), 1800)
     */
    public static record DayBucket(
            @Schema(description = "날짜", example = "2025-09-28")
            LocalDate date,
            @Schema(description = "해당 날짜의 총 주행 시간(초)", example = "1800")
            int seconds
    ) {}

    /**
     * 🚘 LastDriving
     *  - 최근 주행 1건에 대한 요약 정보
     *  - 목록에서 상단 카드나 홈 위젯으로 노출할 때 사용
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LastDriving {

        @Schema(description = "주행 기록 ID", example = "321")
        private Long recordId;

        @Schema(description = "시작 시각", example = "2025-09-28T10:00:00")
        private LocalDateTime startTime;

        @Schema(description = "종료 시각", example = "2025-09-28T10:45:12")
        private LocalDateTime endTime;

        @Schema(description = "총 주행 시간(초)", example = "2712")
        private Integer totalSeconds;

        @Schema(description = "운전 점수(0~100, null 가능)", example = "87.5")
        private Float score;
    }
}
