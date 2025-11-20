package com.drivingcoach.backend.domain.home.domain.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HomeRecentRecordResponse {
    private Long drivingId;
    private int startYear;
    private int startMonth;
    private int startDay;
    private String startTime;         // "04:07"
    private int drivingTime;          // 분 단위? 초 단위? (명세 예시 60 -> 분으로 가정 혹은 초)
    private String drivingScoreMessage; // "안전", "주의" 등
}