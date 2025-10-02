package com.drivingcoach.backend.domain.driving.domain.entity;

import com.drivingcoach.backend.domain.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ DrivingRecord (운전기록) 엔티티
 *
 * ERD 매핑
 *  - id               : PK
 *  - user_id          : 사용자 FK → User.id
 *  - start_time       : 주행 시작 시각
 *  - end_time         : 주행 종료 시각 (종료 전이면 null 가능)
 *  - total_time       : 주행 총 시간(초 단위). end_time 설정 시 자동 계산 헬퍼 제공
 *  - score            : 운전 점수(0.0~100.0 권장). 분석이 끝나면 저장
 *  - video_url        : 주행 영상(또는 zip/청크 메타) S3 위치. 문자열 키/URL
 *
 * 부가 구성
 *  - events           : 운전 중 발생한 이벤트(급가속, 신호위반 등) 목록. 양방향 @OneToMany
 *  - createdAt/updatedAt : JPA Auditing
 *
 * 주의
 *  - JPA Auditing 사용을 위해 @EnableJpaAuditing 필요 (JpaConfig 참고)
 *  - totalTime(초)는 비즈니스 로직에서 end_time 설정 시 계산하는 헬퍼 제공
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(name = "driving_record",
        indexes = {
                @Index(name = "idx_driving_record_user_id", columnList = "user_id"),
                @Index(name = "idx_driving_record_start_time", columnList = "start_time")
        })
@EntityListeners(AuditingEntityListener.class)
public class DrivingRecord {

    /* ========================= 기본 컬럼들 ========================= */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    /**
     * 다대일(ManyToOne) 관계: 여러 개의 DrivingRecord 가 한 명의 User에 속함
     * - 지연로딩(LAZY): 필요할 때만 User를 불러옴 (성능 최적화)
     * - nullable=false: 반드시 사용자에 귀속되어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("사용자 FK")
    private User user;

    @Column(name = "start_time", nullable = false)
    @Comment("주행 시작 시각")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @Comment("주행 종료 시각(진행 중이면 null)")
    private LocalDateTime endTime;

    @Column(name = "total_time", nullable = false)
    @Comment("주행 총 시간(초). 종료 시각 설정 시 헬퍼로 자동 계산 권장")
    @Builder.Default
    private Integer totalTime = 0;

    @Column(name = "score")
    @Comment("운전 점수(0~100 권장). 분석 완료 후 저장")
    private Float score;

    @Column(name = "video_url", length = 512)
    @Comment("주행 영상 S3 키/URL (또는 zip, 청크메타 위치)")
    private String videoUrl;

    /* ========================= 이벤트 연관관계 ========================= */

    /**
     * 양방향 연관관계(편의 메서드 포함)
     * - mappedBy = "drivingRecord" : DrivingEvent.drivingRecord 가 연관관계의 주인
     * - CascadeType.ALL            : 기록 생성/삭제 시 이벤트도 같이 처리 (요구사항에 따라 조정)
     * - orphanRemoval = true       : events 리스트에서 제거되면 DB에서도 삭제
     * - LAZY                       : 필요 시에만 이벤트 로딩 (대량 이벤트 대비)
     */
    @OneToMany(mappedBy = "drivingRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DrivingEvent> events = new ArrayList<>();

    /* ========================= 감사(Auditing) 컬럼 ========================= */

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /* ========================= 연관관계 편의 메서드 ========================= */

    /**
     * 이벤트를 현재 기록에 추가하면서 양방향 관계를 일관되게 유지
     */
    public void addEvent(DrivingEvent event) {
        this.events.add(event);
        event.setDrivingRecord(this);
    }

    /**
     * 이벤트를 현재 기록에서 제거하면서 양방향 관계 정리
     */
    public void removeEvent(DrivingEvent event) {
        this.events.remove(event);
        event.setDrivingRecord(null);
    }

    /* ========================= 비즈니스 메서드 ========================= */

    /**
     * 주행 종료 시각을 설정하고 totalTime(초)을 자동 계산
     * - startTime 이 없으면 IllegalStateException
     * - endTime 이 startTime 이전이면 IllegalArgumentException
     */
    public void endDriving(LocalDateTime endTime) {
        if (this.startTime == null) {
            throw new IllegalStateException("startTime 이 설정되지 않았습니다.");
        }
        if (endTime.isBefore(this.startTime)) {
            throw new IllegalArgumentException("endTime 이 startTime 보다 이전일 수 없습니다.");
        }
        this.endTime = endTime;

        // Duration 을 초 단위 정수로 변환하여 저장
        long seconds = Duration.between(this.startTime, endTime).getSeconds();
        if (seconds < 0) seconds = 0; // 방어적 코드
        this.totalTime = (int) seconds;
    }

    /**
     * 분석 결과로 산출된 운전 점수를 저장
     * - 점수 범위 체크는 요구사항에 맞추어 조정(여기서는 0~100 권장)
     */
    public void updateScore(Float score) {
        if (score != null) {
            if (score < 0f) score = 0f;
            if (score > 100f) score = 100f;
            this.score = score;
        }
    }

    /**
     * 주행 영상(또는 zip/청크 메타) S3 키/URL 설정
     */
    public void updateVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
