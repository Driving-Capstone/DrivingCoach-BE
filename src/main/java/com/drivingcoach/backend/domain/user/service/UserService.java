package com.drivingcoach.backend.domain.user.service;

import com.drivingcoach.backend.domain.driving.repository.DrivingEventRepository;
import com.drivingcoach.backend.domain.driving.repository.DrivingRecordRepository;
import com.drivingcoach.backend.domain.user.domain.dto.request.ChangePasswordRequest;
import com.drivingcoach.backend.domain.user.domain.dto.request.UpdateUserProfileRequest;
import com.drivingcoach.backend.domain.user.domain.dto.response.MyPageInfoResponse;
import com.drivingcoach.backend.domain.user.domain.dto.response.UserProfileResponse;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.exception.CustomException;
import com.drivingcoach.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DrivingRecordRepository drivingRecordRepository;
    private final DrivingEventRepository drivingEventRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = getActiveUserOrThrow(userId);

        // 1. 기본 유저 정보 변환
        UserProfileResponse response = UserProfileResponse.from(user);

        // 2. 통계 데이터 조회 및 계산 (마이페이지 로직 재사용)
        long totalDriving = drivingRecordRepository.countByUserId(userId);
        Long totalTimeSec = drivingRecordRepository.sumTotalTimeByUserId(userId);
        Double avgScore = drivingRecordRepository.findAverageScoreByUserId(userId);

        // [수정 후] 변환 없이 '초(Seconds)' 단위 그대로 전달
        // (프론트엔드의 formatTotalDrivingTime 함수가 초 단위를 기대하므로)
        double totalTimeSeconds = (totalTimeSec != null) ? totalTimeSec.doubleValue() : 0.0;

        float safeScore = (avgScore != null) ? avgScore.floatValue() : 0f;

        // 3. DTO에 통계 정보 설정
        response.setTotalDrivingCount(totalDriving);
        response.setTotalDrivingTime(totalTimeSeconds);
        response.setSafeScore(safeScore);

        return response;
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getActiveUserOrThrow(userId);

        user.updateProfile(
                request.getNickname(),
                request.getGender(),
                request.getBirthDate()
        );

        // 변경감지로 flush, 혹은 명시적 save
        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getActiveUserOrThrow(userId);

        // 현재 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 동일 비밀번호 방지 (선택)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_CANNOT_BE_SAME);
        }

        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
        userRepository.save(user);
    }

    @Transactional
    public void deactivate(Long userId) {
        User user = getActiveUserOrThrow(userId);
        user.deactivate();
        userRepository.save(user);
    }

    /* ====== Internal Helpers ====== */

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public MyPageInfoResponse getMyPageInfo(Long userId) {
        User user = getActiveUserOrThrow(userId);

        // DrivingRecordRepository에서 통계 데이터 조회
        long totalDriving = drivingRecordRepository.countByUserId(userId);
        Long totalTimeSec = drivingRecordRepository.sumTotalTimeByUserId(userId);
        Double avgScore = drivingRecordRepository.findAverageScoreByUserId(userId);

        // (추가!) 총 이벤트 수 조회
        long totalEvents = drivingEventRepository.countAllEventsByUserId(userId);

        // 초 -> 시간 변환 (소수점 한자리)
        double totalTimeHours = (totalTimeSec != null) ? Math.round((totalTimeSec / 3600.0) * 10) / 10.0 : 0.0;
        float safeScore = (avgScore != null) ? avgScore.floatValue() : 0f;

        return MyPageInfoResponse.builder()
                .allDriving(totalDriving)
                .allTime(totalTimeHours)
                .safeScore(safeScore)
                .totalEvents(totalEvents) // (추가!)
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .joinDay(user.getCreatedAt().toLocalDate())
                .build();
    }

    public boolean checkPassword(Long userId, String rawPassword) {
        User user = getActiveUserOrThrow(userId);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
