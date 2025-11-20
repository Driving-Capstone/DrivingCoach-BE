package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryDetailResponse {
    // 기존 HistoryResponse 필드들 포함
    private Long drivingId;
    private int startYear;
    private int startMonth;
    private int startDay;
    private String startTime;
    private int drivingTime;
    private String drivingScoreMessage;
    private int eventTime;

    // 상세 추가 필드
    private String videoUrl; // "주행 총 영상"
    private List<SimpleEventDto> eventList;

    @Getter @Builder
    public static class SimpleEventDto {
        private String eventMessage; // "앞차와의 거리가..."
        private String time;         // "00:05:23"
    }

    public static HistoryDetailResponse from(DrivingRecord record) {
        // (중복 로직은 메서드 추출 가능)
        String msg = (record.getScore() != null && record.getScore() >= 80) ? "안전" : "보통"; // 로직 단순화

        List<SimpleEventDto> events = record.getEvents().stream().map(e ->
                SimpleEventDto.builder()
                        .eventMessage(e.getEventType() + " 감지됨") // 메시지 로직 필요
                        .time(e.getEventTime().toLocalTime().toString()) // 포맷팅 필요 시 수정
                        .build()
        ).collect(Collectors.toList());

        return HistoryDetailResponse.builder()
                .drivingId(record.getId())
                .startYear(record.getStartTime().getYear())
                .startMonth(record.getStartTime().getMonthValue())
                .startDay(record.getStartTime().getDayOfMonth())
                .startTime(String.format("%02d:%02d", record.getStartTime().getHour(), record.getStartTime().getMinute()))
                .drivingTime(record.getTotalTime() / 60)
                .drivingScoreMessage(msg)
                .eventTime(events.size())
                .videoUrl(record.getVideoUrl())
                .eventList(events)
                .build();
    }
}