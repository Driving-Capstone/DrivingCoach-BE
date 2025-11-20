package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryResponse {
    private Long drivingId;
    private int startYear;
    private int startMonth;
    private int startDay;
    private String startTime;
    private int drivingTime;       // 명세서 예시: 60
    private String drivingScoreMessage;
    private int eventTime;         // 이벤트 발생 횟수

    // Entity -> DTO 변환 메서드
    public static HistoryResponse from(DrivingRecord record) {
        // 점수에 따른 메시지 로직 (예시)
        String msg = "안전";
        if (record.getScore() != null && record.getScore() < 70) msg = "주의";
        else if (record.getScore() != null && record.getScore() < 40) msg = "위험";

        return HistoryResponse.builder()
                .drivingId(record.getId())
                .startYear(record.getStartTime().getYear())
                .startMonth(record.getStartTime().getMonthValue())
                .startDay(record.getStartTime().getDayOfMonth())
                .startTime(String.format("%02d:%02d", record.getStartTime().getHour(), record.getStartTime().getMinute()))
                .drivingTime(record.getTotalTime() / 60) // 초 -> 분 변환 예시
                .drivingScoreMessage(msg)
                .eventTime(record.getEvents() != null ? record.getEvents().size() : 0)
                .build();
    }
}