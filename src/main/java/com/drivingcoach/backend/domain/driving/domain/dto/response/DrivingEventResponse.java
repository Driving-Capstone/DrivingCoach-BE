package com.drivingcoach.backend.domain.driving.domain.dto.response;

import com.drivingcoach.backend.domain.driving.domain.entity.DrivingEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ DrivingEventResponse
 *
 * 용도
 *  - 운전 이벤트(급가속/감속/차선이탈 등)를 API 응답으로 내려줄 때 사용하는 DTO입니다.
 *  - 엔티티(DrivingEvent)를 직접 노출하지 않고, 필요한 필드만 안전하게 전달합니다.
 *
 * 포함 필드
 *  - id         : 이벤트 PK
 *  - recordId   : 소속 주행기록 ID (클라이언트가 화면 전환/요청에 활용)
 *  - type       : 이벤트 유형 (문자열)
 *  - eventTime  : 이벤트 발생 시각
 *  - severity   : 심각도 (문자열; 초기 단순화, 차후 Enum 승격 가능)
 *  - note       : (선택) 메모
 *  - createdAt  : 생성 시각 (감사/정렬/디버깅용)
 *  - updatedAt  : 수정 시각
 *
 * 팁
 *  - from(DrivingEvent) 정적 팩토리 메서드로 Entity→DTO 변환을 표준화합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrivingEventResponse {

    @Schema(description = "이벤트 ID", example = "101")
    private Long id;

    @Schema(description = "주행 기록 ID", example = "12")
    private Long recordId;

    @Schema(description = "이벤트 유형", example = "lane_departure")
    private String type;

    @Schema(description = "이벤트 발생 시각", example = "2025-09-28T10:12:30")
    private LocalDateTime eventTime;

    @Schema(description = "심각도", example = "high")
    private String severity;

    @Schema(description = "메모(선택)", example = "차로 이탈 감지됨(우측)")
    private String note;

    @Schema(description = "생성 시각", example = "2025-09-28T10:12:31")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각", example = "2025-09-28T10:12:31")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환 헬퍼
     */
    public static DrivingEventResponse from(DrivingEvent e) {
        return DrivingEventResponse.builder()
                .id(e.getId())
                .recordId(e.getDrivingRecord() != null ? e.getDrivingRecord().getId() : null)
                .type(e.getEventType())
                .eventTime(e.getEventTime())
                .severity(e.getSeverity())
                .note(e.getNote())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
