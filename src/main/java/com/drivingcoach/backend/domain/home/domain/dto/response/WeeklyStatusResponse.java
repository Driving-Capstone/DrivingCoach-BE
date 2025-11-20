package com.drivingcoach.backend.domain.home.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyStatusResponse {

    @Schema(description = "기간 시작 (포함)")
    private LocalDateTime from;

    @Schema(description = "기간 끝 (미포함)")
    private LocalDateTime to;

    @Schema(description = "기간 내 총 주행 시간(초)")
    private int totalSeconds;

    @Schema(description = "기간 내 총 주행 횟수") // (추가)
    private long totalDrivingCount;

    @Schema(description = "기간 내 총 이벤트 발생 횟수") // (추가)
    private long totalEventCount;

    @Schema(description = "기간 내 평균 점수 (null 가능)")
    private Double averageScore;

    @Schema(description = "일자별 총 주행(초) 버킷")
    private List<DayBucket> dailySeconds;

    @Schema(description = "가장 최근 주행 요약(선택)")
    private LastDriving lastDriving;

    // ... (DayBucket, LastDriving 내부 클래스는 기존 코드 그대로 유지) ...
    public static record DayBucket(LocalDate date, int seconds) {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LastDriving {
        private Long recordId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer totalSeconds;
        private Float score;
    }
}