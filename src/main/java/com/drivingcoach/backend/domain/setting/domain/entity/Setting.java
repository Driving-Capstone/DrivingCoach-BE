package com.drivingcoach.backend.domain.setting.domain.entity;

import com.drivingcoach.backend.domain.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ✅ Setting (사용자 환경설정) 엔티티
 *
 * 역할
 *  - 앱에서 제공하는 피드백/알림 관련 사용자 개별 설정을 저장합니다.
 *  - 예: 진동 사용 여부, 음성 피드백 방식 등
 *
 * 설계 포인트
 *  1) 1:1 관계(User당 1행)로 설계 — @OneToOne + unique FK 인덱스
 *     - 선택적으로 User 생성 시 기본 설정을 함께 생성하는 것이 UX 상 유리합니다.
 *  2) 컬럼은 추후 손쉽게 추가/변경될 수 있도록 boolean, enum(String) 위주로 구성
 *  3) JPA Auditing(@CreatedDate/@LastModifiedDate)로 생성/수정시각 자동 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(name = "user_setting",
        indexes = {
                @Index(name = "ux_user_setting_user", columnList = "user_id", unique = true)
        })
@EntityListeners(AuditingEntityListener.class)
public class Setting {

    /* ===================== 기본키 ===================== */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    /* ===================== 연관관계 (User 1:1) ===================== */

    /**
     * User : Setting = 1 : 1
     * - 유저당 1개의 설정 레코드만 존재
     * - LAZY 로딩으로 필요할 때만 User를 불러옴
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("사용자 FK(Unique)")
    private User user;

    /* ===================== 실제 설정 값들 ===================== */

    @Column(name = "vibration_enabled", nullable = false)
    @Comment("진동 알림 사용 여부")
    @Builder.Default
    private boolean vibrationEnabled = true;

    @Column(name = "feedback_voice", nullable = false, length = 30)
    @Comment("음성 피드백 설정 (예: off / male / female / tts_ko 등)")
    @Builder.Default
    private String feedbackVoice = "off";

    // 필요 시 추가: 야간모드, 단위(km/mi), 푸시 알림 허용 등
    // @Column(name = "dark_mode", nullable = false) @Builder.Default private boolean darkMode = false;

    /* ===================== 감사(Auditing) ===================== */

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /* ===================== 비즈니스 메서드 ===================== */

    /** 진동 알림 on/off 토글 또는 지정 */
    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    /** 음성 피드백 모드 변경 */
    public void setFeedbackVoice(String mode) {
        if (mode == null || mode.isBlank()) return;
        this.feedbackVoice = mode;
    }
}
