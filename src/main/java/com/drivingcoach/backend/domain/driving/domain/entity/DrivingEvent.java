package com.drivingcoach.backend.domain.driving.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ✅ DrivingEvent (운전 중 이벤트) 엔티티
 *
 * ERD 대응
 *  - id              : PK
 *  - driving_record_id : FK → DrivingRecord.id (여러 이벤트가 하나의 기록에 소속)
 *  - event_type      : 이벤트 종류 (예: 급가속, 급감속, 차선이탈, 신호위반 등)
 *  - event_time      : 이벤트가 발생한 순간(서버 기준 or 클라이언트 기준, 정책에 맞게)
 *  - severity        : 심각도(예: low / medium / high) — 문자열로 저장(초기 단순화)
 *  - note            : (선택) 추가 설명/디버깅용 메모
 *
 * 설계 메모
 *  - event_type / severity 는 초기에 문자열로 두고, 이후 Enum 으로 리팩터링하기 쉬운 형태로 구성
 *  - 대량 조회를 고려하여 driving_record_id + event_time 에 인덱스 부여
 *  - 이벤트는 영상 타임라인과 싱크되므로 event_time 기준 정렬 조회가 자주 발생
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(name = "driving_event",
        indexes = {
                @Index(name = "idx_driving_event_record", columnList = "driving_record_id"),
                @Index(name = "idx_driving_event_time", columnList = "event_time")
        })
@EntityListeners(AuditingEntityListener.class)
public class DrivingEvent {

    /* ============== 기본키 ============== */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    /* ============== 연관관계 ============== */

    /**
     * 다대일(ManyToOne): 여러 이벤트가 한 개의 운전기록에 속함
     * - LAZY 로딩: 이벤트 목록만 불러올 때 DrivingRecord 까지 즉시 로딩하지 않음(성능)
     * - nullable=false: 반드시 어떤 DrivingRecord 에 귀속되어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driving_record_id", nullable = false)
    @Comment("소속 운전기록 FK")
    private DrivingRecord drivingRecord;

    /* ============== 이벤트 속성 ============== */

    @Column(name = "event_type", nullable = false, length = 50)
    @Comment("이벤트 종류(예: 급가속, 급감속, 차선이탈, 신호위반 등)")
    private String eventType;

    @Column(name = "event_time", nullable = false)
    @Comment("이벤트 발생 시각")
    private LocalDateTime eventTime;

    @Column(name = "severity", nullable = false, length = 20)
    @Comment("심각도(low/medium/high 등)")
    private String severity;

    @Column(name = "note", length = 300)
    @Comment("추가 메모/설명(선택)")
    private String note;

    /* ============== 감사(Auditing) ============== */

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /* ============== 연관관계 편의 메서드 ============== */

    /**
     * 양방향 연관관계 세팅을 위한 편의 메서드
     * - DrivingRecord.addEvent(...) 와 함께 사용 권장
     */
    public void setDrivingRecord(DrivingRecord record) {
        this.drivingRecord = record;
    }

    /* ============== 비즈니스 메서드 ============== */

    /**
     * 이벤트의 세부 정보를 수정(패치)하는 헬퍼
     * - null/blank 가 아닌 값만 반영 → 부분 수정(PATCH) 시 유용
     */
    public void patch(String eventType, LocalDateTime eventTime, String severity, String note) {
        if (eventType != null && !eventType.isBlank()) this.eventType = eventType;
        if (eventTime != null) this.eventTime = eventTime;
        if (severity != null && !severity.isBlank()) this.severity = severity;
        if (note != null) this.note = note;
    }
}
