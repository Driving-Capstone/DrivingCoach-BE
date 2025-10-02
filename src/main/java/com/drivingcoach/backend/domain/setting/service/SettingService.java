package com.drivingcoach.backend.domain.setting.service;

import com.drivingcoach.backend.domain.setting.domain.entity.Setting;
import com.drivingcoach.backend.domain.setting.repository.SettingRepository;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ SettingService
 *
 * 역할
 *  - 사용자 환경설정(Setting)의 조회/생성/수정 비즈니스 로직을 담당합니다.
 *  - "존재하지 않으면 생성하고, 있으면 갱신한다"는 Upsert 패턴을 지원합니다.
 *
 * 트랜잭션
 *  - 클래스 기본: readOnly = true (조회 성능 최적화)
 *  - 생성/수정 작업은 각 메서드에서 @Transactional(readOnly = false)로 오버라이드합니다.
 *
 * 예외
 *  - 비활성/존재하지 않는 사용자에 대해서는 USER_NOT_FOUND 예외를 던집니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingService {

    private final SettingRepository settingRepository;
    private final UserRepository userRepository;

    /* ======================== 조회 ======================== */

    /**
     * 내 설정 조회
     * - 없으면 기본값으로 생성한 뒤 반환할지, 404로 응답할지는 정책에 따라 다릅니다.
     *   여기서는 "없으면 생성해서 반환" 전략을 사용합니다(앱 UX 단순화).
     */
    @Transactional
    public Setting getMySettingOrCreate(Long userId) {
        return settingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = getActiveUserOrThrow(userId);
                    Setting created = Setting.builder()
                            .user(user)
                            .vibrationEnabled(true) // 기본값
                            .feedbackVoice("off")   // 기본값
                            .build();
                    settingRepository.save(created);
                    log.info("[SETTING] created default for userId={}", userId);
                    return created;
                });
    }

    /**
     * 내 설정 조회(존재하지 않으면 예외)
     * - 기본값 자동 생성을 원치 않는 화면/엔드포인트에서 사용하세요.
     */
    public Setting getMySettingOrThrow(Long userId) {
        return settingRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "설정을 찾을 수 없습니다."));
    }

    /* ======================== 변경 ======================== */

    /**
     * 진동 알림 on/off 변경 (Upsert)
     * - 레코드가 없으면 생성 후 값 반영
     */
    @Transactional
    public Setting updateVibration(Long userId, boolean enabled) {
        Setting s = getMySettingOrCreate(userId);
        s.setVibrationEnabled(enabled);
        return settingRepository.save(s);
    }

    /**
     * 음성 피드백 모드 변경 (Upsert)
     * - 예: "off", "male", "female", "tts_ko" 등
     */
    @Transactional
    public Setting updateFeedbackVoice(Long userId, String voiceMode) {
        if (voiceMode == null || voiceMode.isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "voiceMode 가 비어 있습니다.");
        }
        Setting s = getMySettingOrCreate(userId);
        s.setFeedbackVoice(voiceMode);
        return settingRepository.save(s);
    }

    /**
     * 전체 설정 일괄 수정 (필요 시 확장)
     * - null/blank 가 아닌 값만 반영하는 패치 스타일로 구현하고 싶다면
     *   DTO 레벨에서 Optional 처리 또는 서비스에서 조건 분기를 두면 됩니다.
     */
    @Transactional
    public Setting upsertAll(Long userId, Boolean vibrationEnabled, String feedbackVoice) {
        Setting s = getMySettingOrCreate(userId);
        if (vibrationEnabled != null) s.setVibrationEnabled(vibrationEnabled);
        if (feedbackVoice != null && !feedbackVoice.isBlank()) s.setFeedbackVoice(feedbackVoice);
        return settingRepository.save(s);
    }

    /* ======================== 내부 헬퍼 ======================== */

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
