package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryResponse {
    private Long id;               // drivingId (프론트 요청: id)
    private String date;           // "2024-01-15 14:30" 포맷
    private String duration;       // "45분" 포맷
    private String distance;       // (현재 DB에 거리 정보 없음. 추후 추가 필요하거나 0km 처리)
    private int events;            // 이벤트 개수
    private String status;         // "안전", "주의", "위험"

    public static HistoryResponse from(DrivingRecord record) {
        // 날짜 포맷팅
        String dateStr = record.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // 주행 시간 포맷팅 (초 -> 분)
        int minutes = record.getTotalTime() / 60;
        String durationStr = minutes + "분";

        // 상태 로직 (점수 기준 예시)
        String statusStr = "보통";
        if (record.getScore() != null) {
            if (record.getScore() >= 80) statusStr = "안전";
            else if (record.getScore() >= 95) statusStr = "매우 안전";
            else if (record.getScore() < 60) statusStr = "주의";
        }

        // 이벤트 개수
        int eventCount = (record.getEvents() != null) ? record.getEvents().size() : 0;

        return HistoryResponse.builder()
                .id(record.getId())
                .date(dateStr)
                .duration(durationStr)
                .distance("0km") // TODO: 거리 정보 수집 시 수정
                .events(eventCount)
                .status(statusStr)
                .build();
    }
}