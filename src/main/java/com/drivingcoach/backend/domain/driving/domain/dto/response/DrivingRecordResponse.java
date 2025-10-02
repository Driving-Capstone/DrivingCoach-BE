package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import com.drivingcoach.backend.domain.driving.domain.entity.DrivingRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ DrivingRecordResponse
 *
 * 용도
 *  - 주행 기록(DrivingRecord)을 API 응답으로 내려줄 때 사용하는 DTO입니다.
 *  - 목록/상세 공용으로 쓰며, 상세 조회에서는 이벤트 목록(events)을 포함할 수 있습니다.
 *
 * 포함 필드
 *  - id           : 주행 기록 PK
 *  - startTime    : 시작 시각
 *  - endTime      : 종료 시각(진행 중이면 null)
 *  - totalTimeSec : 총 주행 시간(초). 서버에서 endDriving 시 자동 계산/저장
 *  - score        : 운전 점수(0~100 권장, null 가능)
 *  - videoUrl     : S3 키/URL 등 영상(또는 zip/청크 메타) 위치
 *  - events       : (선택) 이벤트 목록. withEvents=true일 때만 채웁니다.
 *
 * 변환 정책
 *  - 엔티티를 직접 노출하지 않고, from(...) 정적 팩토리로 안전하게 매핑합니다.
 *  - withEvents=false(기본) → 목록 조회 등에서 가볍게 사용
 *  - withEvents=true       → 상세 화면에서 이벤트 타임라인까지 한 번에 제공
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrivingRecordResponse {

    @Schema(description = "주행 기록 ID", example = "12")
    private Long id;

    @Schema(description = "시작 시각", example = "2025-09-28T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "종료 시각", example = "2025-09-28T10:45:12")
    private LocalDateTime endTime;

    @Schema(description = "총 주행 시간(초)", example = "2712")
    private Integer totalTimeSec;

    @Schema(description = "운전 점수(0~100, null 가능)", example = "87.5")
    private Float score;

    @Schema(description = "영상/zip의 S3 키 또는 URL", example = "driving/uuid/final.zip")
    private String videoUrl;

    @Schema(description = "이벤트 목록 (상세 조회 시 포함)")
    private List<DrivingEventResponse> events;

    /* ------------------ Entity → DTO 변환 ------------------ */

    /**
     * 목록/상세 공용 변환 메서드
     * @param record     DrivingRecord 엔티티
     * @param withEvents true면 events 필드를 함께 채움
     */
    public static DrivingRecordResponse from(DrivingRecord record, boolean withEvents) {
        DrivingRecordResponse dto = DrivingRecordResponse.builder()
                .id(record.getId())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .totalTimeSec(record.getTotalTime())
                .score(record.getScore())
                .videoUrl(record.getVideoUrl())
                .build();

        if (withEvents) {
            List<DrivingEvent> eventEntities = record.getEvents(); // LAZY 주의: 서비스에서 fetch join 사용 권장
            if (eventEntities != null) {
                dto.setEvents(eventEntities.stream()
                        .map(DrivingEventResponse::from)
                        .toList());
            }
        }
        return dto;
    }
}
