package com.drivingcoach.backend.domain.home.domain.dto.response;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HomeMonthStatusResponse {
    private int totalDriving;     // 총 주행 횟수
    private double drivingHours;  // 주행 시간 (예: 3.2h)
    private int warningCount;     // 경고 알림 횟수
}
