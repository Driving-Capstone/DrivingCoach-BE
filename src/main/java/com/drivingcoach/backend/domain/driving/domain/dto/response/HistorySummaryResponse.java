package com.drivingcoach.backend.domain.driving.domain.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistorySummaryResponse {
    private long totalDrivingCount;   // 총 주행 횟수
    private String totalDrivingTime;  // 총 주행 시간 ("5.2시간")
    private long totalEventCount;     // 총 이벤트 수
}